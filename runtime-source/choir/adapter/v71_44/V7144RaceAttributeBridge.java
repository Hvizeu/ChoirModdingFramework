package choir.adapter.v71_44;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import choir.api.race.RaceMissingTargetPolicy;
import choir.api.race.RaceNumericAttribute;
import choir.api.race.RaceNumericPatch;
import choir.api.race.RaceRelationshipPatch;
import choir.api.race.RaceStandingPatch;
import choir.api.race.RaceTextField;
import choir.api.race.RaceTextPatch;
import choir.internal.ChoirDiagnostics;
import choir.internal.race.RaceAttributeRegistry;
import choir.internal.race.RaceAttributeRegistry.AttributePlan;
import choir.internal.race.RaceAttributeRegistry.NumericTargetPlan;
import choir.internal.race.RaceAttributeRegistry.RelationshipTargetPlan;
import choir.internal.race.RaceAttributeRegistry.StandingTargetPlan;
import choir.internal.race.RaceAttributeRegistry.TextTargetPlan;
import choir.internal.race.RaceRegistry;
import init.race.RACES;
import init.race.Race;
import init.race.RaceInfo;
import init.race.RacePopulation;
import init.race.RacePreferrence;
import init.race.RaceServiceSorter;
import init.race.RaceStats;
import init.resources.RESOURCE;
import init.resources.RESOURCES;
import init.type.BUILDING_PREF;
import init.type.BUILDING_PREFS;
import init.type.CLIMATE;
import init.type.CLIMATES;
import init.type.CRIMES;
import init.type.CRIME_PUNISHMENTS;
import init.type.HCLASS;
import init.type.HCLASSES;
import init.type.TERRAIN;
import init.type.TERRAINS;
import settlement.main.SETT;
import settlement.room.infra.elderly.ROOM_RESTHOME;
import settlement.room.main.RoomBlueprint;
import settlement.room.main.RoomBlueprintIns;
import settlement.room.main.employment.RoomEmploymentSimple;
import settlement.room.water.pool.ROOM_POOL;
import settlement.stats.STATS;
import settlement.stats.colls.StatsBurial.StatGrave;
import settlement.stats.service.StatServiceImp;
import settlement.stats.standing.StatStanding.StandingDef;
import settlement.stats.stat.STAT;
import settlement.tilemap.floor.Floors.Floor;
import snake2d.util.file.Json;

/** Fingerprint-gated V71.44 materializer for the public race attribute suite. */
final class V7144RaceAttributeBridge {
	private static boolean compatible;
	private static long lastGeneration;
	private static int lastRegistryIdentity;
	private static final Map<String,Field> fields = new HashMap<String,Field>();
	private V7144RaceAttributeBridge() { }

	static void initialize() {
		V7144RaceTargetFingerprint.Result result = V7144RaceTargetFingerprint.verify();
		compatible = result.matches;
		try {
			for (String name : new String[] {"desc_long","initialChallenge","pros","cons","armyNames","raiderNames","sHello","sGoodbye","sCurse","sInsult","sInsulting","sLord","sCity","sOthers","sSelves","sSelf","sChildren"}) field(RaceInfo.class,name);
			for (String name : new String[] {"growth","max","climates","maxClimate","terrains","maxTerrain"}) field(RacePopulation.class,name);
			for (String name : new String[] {"structure","pools","work","others","priceCaps","priceMuls","crimeFreedom","crimeLaw","punishment","resthomes","mostHated"}) field(RacePreferrence.class,name);
			for (String name : new String[] {"reps","repNormalized","standings"}) field(RaceStats.class,name);
			field(Race.class,"service"); field(Race.class,"bio");
			field(StandingDef.class,"inverted"); field(StandingDef.class,"mul"); field(StandingDef.class,"expo"); field(StandingDef.class,"exp"); field(StandingDef.class,"child"); field(StandingDef.class,"data");
			Class<?> bio = Class.forName("init.race.bio.Bio"); Class<?> opinion = Class.forName("init.race.bio.BioOpinion");
			field(bio,"improve"); field(opinion,"datas");
		} catch (Exception failure) {
			compatible = false;
			ChoirDiagnostics.error("RACE attribute-reflection-check-failed cause=" + failure.getClass().getSimpleName() + ": " + failure.getMessage());
		}
		RaceAttributeRegistry.adapterReady(compatible);
		ChoirDiagnostics.info("RACE attribute-compatibility matched=" + compatible + " gameJar=" + result.jar + " targets=" + result.actual);
	}

	static synchronized void materialize(long runtimeGeneration) {
		AttributePlan plan = RaceAttributeRegistry.plan();
		int registryIdentity = System.identityHashCode(RACES.all());
		if (lastGeneration == runtimeGeneration && lastRegistryIdentity == registryIdentity)
			throw new IllegalStateException("Race attribute materialization attempted twice for runtime generation " + runtimeGeneration);
		lastGeneration = runtimeGeneration; lastRegistryIdentity = registryIdentity;
		if (!compatible) throw new IllegalStateException("The fingerprint-gated v71.44 RACES lifecycle bridge is unavailable because an original target did not match.");
		Map<String,Race> races = races(); ArrayList<String> targets = new ArrayList<String>(); Set<Race> standingRaces = new HashSet<Race>(); Set<Race> relationshipRaces = new HashSet<Race>(); Set<Race> workRaces = new HashSet<Race>();

		for (TextTargetPlan target : plan.text) {
			Race race = races.get(target.raceId);
			if (race == null) { if (requiredText(target)) throw missing(target.targetId); skipped(target.targetId,"race-missing"); continue; }
			List<String> base = textValues(race.info, target.field);
			List<String> effective = RaceAttributeRegistry.resolve(target, base).values;
			writeText(race.info, target.field, effective); targets.add(target.targetId);
			ChoirDiagnostics.info("RACE text-materialized runtime.generation="+runtimeGeneration+" target="+target.targetId+" base.count="+base.size()+" effective.count="+effective.size()+" contributors="+textContributors(target));
		}
		for (NumericTargetPlan target : plan.numeric) {
			Race race = races.get(target.raceId);
			if (race == null) { if (requiredNumeric(target)) throw missing(target.targetId); skipped(target.targetId,"race-missing"); continue; }
			NumericAccess access = numericAccess(race,target);
			if (access == null) { if (requiredNumeric(target)) throw new IllegalStateException("Race numeric subject is missing: "+target.targetId); skipped(target.targetId,"subject-missing"); continue; }
			double effective = RaceAttributeRegistry.resolve(target,access.get()).value; access.set(effective); targets.add(target.targetId);
			if (target.attribute==RaceNumericAttribute.WORK_PREFERENCE) workRaces.add(race);
			ChoirDiagnostics.info("RACE numeric-materialized runtime.generation="+runtimeGeneration+" target="+target.targetId+" effective="+effective+" contributors="+numericContributors(target));
		}
		for (RelationshipTargetPlan target : plan.relationships) {
			Race race=races.get(target.raceId), other=races.get(target.otherRaceId);
			if (race==null||other==null) { if(requiredRelationship(target))throw missing(target.targetId); skipped(target.targetId,"optional-race-missing");continue; }
			double[] values=(double[])get(field(RacePreferrence.class,"others"),race.pref());
			double effective=RaceAttributeRegistry.resolve(target,values[other.index()]).value; values[other.index()]=effective;relationshipRaces.add(race);targets.add(target.targetId);
			ChoirDiagnostics.info("RACE relationship-materialized runtime.generation="+runtimeGeneration+" target="+target.targetId+" effective="+effective+" contributors="+relationshipContributors(target));
		}
		for (StandingTargetPlan target : plan.standings) {
			Race race=races.get(target.raceId); STAT stat=stat(target.statId); HCLASS clas=hclass(target.humanoidClassId);
			if(race==null||stat==null||clas==null){if(requiredStanding(target))throw new IllegalStateException("Race standing target is missing: "+target.targetId);skipped(target.targetId,"target-missing");continue;}
			StandingDef[] reps=(StandingDef[])get(field(RaceStats.class,"reps"),race.stats()); StandingDef base=reps[stat.index()];
			RaceAttributeRegistry.StandingResolution resolution=RaceAttributeRegistry.resolve(target,base.get(clas).max,base.inverted);
			reps[stat.index()]=cloneStanding(base,clas,resolution.value,resolution.inverted);standingRaces.add(race);targets.add(target.targetId);
			ChoirDiagnostics.info("RACE standing-materialized runtime.generation="+runtimeGeneration+" target="+target.targetId+" effective="+resolution.value+" inverted="+resolution.inverted+" contributors="+standingContributors(target));
		}
		for(Race race:relationshipRaces) rebuildMostHated(race,races.values());
		for(Race race:workRaces) rebuildResthomes(race);
		for(Race race:standingRaces) rebuildStandingDerived(race);
		RaceRegistry.attributesMaterialized(runtimeGeneration,targets);
		ChoirDiagnostics.info("RACE attribute-registry runtime.generation="+runtimeGeneration+" registryIdentity="+registryIdentity+" text="+plan.text.size()+" numeric="+plan.numeric.size()+" relationships="+plan.relationships.size()+" standings="+plan.standings.size()+" materialized="+targets.size()+" state=CONFIRMED");
	}

	private static List<String> textValues(RaceInfo info,RaceTextField type){Object value=get(field(RaceInfo.class,textField(type)),info);ArrayList<String> out=new ArrayList<>();if(value instanceof CharSequence)out.add(value.toString());else if(value instanceof String[])for(String s:(String[])value)out.add(s);else if(value instanceof CharSequence[])for(CharSequence s:(CharSequence[])value)out.add(s.toString());else if(value instanceof Iterable<?>)for(Object s:(Iterable<?>)value)out.add(s.toString());else throw new IllegalStateException("Unsupported race text storage: "+type);return out;}
	private static void writeText(RaceInfo info,RaceTextField type,List<String> values){Field f=field(RaceInfo.class,textField(type));if(type==RaceTextField.LONG_DESCRIPTION||type==RaceTextField.INITIAL_CHALLENGE)set(f,info,String.join("\n\n",values));else if(type==RaceTextField.ARMY_NAMES)set(f,info,new snake2d.util.sets.ArrayList<String>(values.toArray(new String[0])));else if(type==RaceTextField.PROS||type==RaceTextField.CONS||type==RaceTextField.RAIDER_NAMES)set(f,info,values.toArray(new String[0]));else set(f,info,values.toArray(new CharSequence[0]));}
	private static String textField(RaceTextField type){return switch(type){case LONG_DESCRIPTION->"desc_long";case INITIAL_CHALLENGE->"initialChallenge";case PROS->"pros";case CONS->"cons";case ARMY_NAMES->"armyNames";case RAIDER_NAMES->"raiderNames";case HELLO->"sHello";case GOODBYE->"sGoodbye";case CURSE->"sCurse";case INSULT->"sInsult";case INSULTING->"sInsulting";case LORD->"sLord";case CITY->"sCity";case OTHERS->"sOthers";case SELVES->"sSelves";case SELF->"sSelf";case CHILDREN->"sChildren";};}

	private static NumericAccess numericAccess(Race race,NumericTargetPlan t){try{return switch(t.attribute){
		case POPULATION_MAX->refAccess(race.population(),field(RacePopulation.class,"max"));case POPULATION_GROWTH->refAccess(race.population(),field(RacePopulation.class,"growth"));
		case CLIMATE_PREFERENCE->{CLIMATE x=null;for(CLIMATE v:CLIMATES.ALL())if(v.key().equals(t.subjectId))x=v;yield x==null?null:arrayAccess(race.population(),field(RacePopulation.class,"climates"),x.index(),()->recomputePopulationMax(race.population(),"climates","maxClimate"));}
		case TERRAIN_PREFERENCE->{TERRAIN x=null;for(TERRAIN v:TERRAINS.ALL())if(v.key().equals(t.subjectId))x=v;yield x==null?null:arrayAccess(race.population(),field(RacePopulation.class,"terrains"),x.index(),()->recomputePopulationMax(race.population(),"terrains","maxTerrain"));}
		case STRUCTURE_PREFERENCE->{BUILDING_PREF x=null;for(BUILDING_PREF v:BUILDING_PREFS.ALL())if(v.key().equals(t.subjectId))x=v;yield x==null?null:arrayAccess(race.pref(),field(RacePreferrence.class,"structure"),x.index(),null);}
		case POOL_PREFERENCE->{ROOM_POOL x=pool(t.subjectId);yield x==null?null:arrayAccess(race.pref(),field(RacePreferrence.class,"pools"),x.typeIndex(),null);}
		case WORK_PREFERENCE->{RoomEmploymentSimple x=employment(t.subjectId);yield x==null?null:arrayAccess(race.pref(),field(RacePreferrence.class,"work"),x.eindex(),null);}
		case ROAD_PREFERENCE->{Floor x=floor(t.subjectId);yield x==null?null:new NumericAccess(){public double get(){return x.pref(race);}public void set(double v){x.prefSet(race,v);}};}
		case CRIME_FREEDOM->{CRIMES.CRIME x=crime(t.subjectId);yield x==null?null:arrayAccess(race.pref(),field(RacePreferrence.class,"crimeFreedom"),x.index(),null);}
		case CRIME_LAW->{CRIMES.CRIME x=crime(t.subjectId);yield x==null?null:arrayAccess(race.pref(),field(RacePreferrence.class,"crimeLaw"),x.index(),null);}
		case PUNISHMENT_PREFERENCE->{CRIME_PUNISHMENTS.PUNISHMENT x=punishment(t.subjectId);yield x==null?null:arrayAccess(race.pref(),field(RacePreferrence.class,"punishment"),x.index(),null);}
		case RESOURCE_PRICE_MULTIPLIER->{RESOURCE x=resource(t.subjectId);yield x==null?null:arrayAccess(race.pref(),field(RacePreferrence.class,"priceMuls"),x.tr().index(),null);}
		case RESOURCE_PRICE_CAP->{RESOURCE x=resource(t.subjectId);yield x==null?null:arrayAccess(race.pref(),field(RacePreferrence.class,"priceCaps"),x.tr().index(),null);}
	};}catch(Exception e){throw new IllegalStateException("Could not resolve numeric race target "+t.targetId,e);}}
	private static NumericAccess refAccess(Object owner,Field f){return new NumericAccess(){public double get(){return ((Number)V7144RaceAttributeBridge.get(f,owner)).doubleValue();}public void set(double v){V7144RaceAttributeBridge.set(f,owner,v);}};}
	private static NumericAccess arrayAccess(Object owner,Field f,int index,Runnable after){double[] a=(double[])get(f,owner);return new NumericAccess(){public double get(){return a[index];}public void set(double v){a[index]=v;if(after!=null)after.run();}};}
	private static void recomputePopulationMax(RacePopulation p,String array,String max){double m=0;for(double v:(double[])get(field(RacePopulation.class,array),p))m=Math.max(m,v);set(field(RacePopulation.class,max),p,m);}
	private static interface NumericAccess{double get();void set(double value);}

	private static StandingDef cloneStanding(StandingDef base,HCLASS changed,double value,boolean inverted){try{StandingDef copy=new StandingDef((Json)null);set(field(StandingDef.class,"inverted"),copy,inverted);set(field(StandingDef.class,"mul"),copy,get(field(StandingDef.class,"mul"),base));set(field(StandingDef.class,"expo"),copy,get(field(StandingDef.class,"expo"),base));set(field(StandingDef.class,"exp"),copy,get(field(StandingDef.class,"exp"),base));set(field(StandingDef.class,"child"),copy,get(field(StandingDef.class,"child"),base));copy.prio=base.prio;Class<?> dataClass=Class.forName("settlement.stats.standing.StatStanding$StandingDef$StandingData");Constructor<?> c=dataClass.getDeclaredConstructor(StandingDef.class,double.class);c.setAccessible(true);Object data=java.lang.reflect.Array.newInstance(dataClass,HCLASSES.ALL().size());Field dismiss=field(dataClass,"dismiss");for(HCLASS h:HCLASSES.ALL()){double v=h==changed?value:base.get(h).max;Object d=c.newInstance(copy,v);dismiss.setBoolean(d,base.get(h).dismiss);java.lang.reflect.Array.set(data,h.index(),d);}set(field(StandingDef.class,"data"),copy,data);return copy;}catch(Exception e){throw new IllegalStateException("Could not clone exact-v71.44 StandingDef",e);}}
	@SuppressWarnings({"rawtypes","unchecked"}) private static void rebuildStandingDerived(Race race){try{RaceStats stats=race.stats();StandingDef[] reps=(StandingDef[])get(field(RaceStats.class,"reps"),stats);double[][] normalized=(double[][])get(field(RaceStats.class,"repNormalized"),stats);snake2d.util.sets.ArrayList standings=new snake2d.util.sets.ArrayList(HCLASSES.ALL().size());for(HCLASS h:HCLASSES.ALL()){double max=0;snake2d.util.sets.ArrayList<STAT> values=new snake2d.util.sets.ArrayList<STAT>(STATS.all().size());for(STAT s:STATS.all()){StandingDef d=reps[s.index()];normalized[h.index()][s.index()]=0;if(d.get(h).max>0){values.add(s);max=Math.max(max,d.get(h).max);}}if(max>0)for(STAT s:STATS.all())if(reps[s.index()].get(h).max>0)normalized[h.index()][s.index()]=reps[s.index()].get(h).max/max;standings.add(values);for(StatServiceImp s:STATS.SERVICE().ALL)s.permission().set(h.get(race),reps[s.total().index()].get(h).max>0);for(StatGrave s:STATS.BURIAL().graves())s.grave().permission().set(h,race,reps[s.index()].get(h).max>0);}set(field(RaceStats.class,"standings"),stats,standings);Constructor<RaceServiceSorter> service=RaceServiceSorter.class.getDeclaredConstructor(Race.class);service.setAccessible(true);set(field(Race.class,"service"),race,service.newInstance(race));rebuildBioOpinion(race);}catch(Exception e){throw new IllegalStateException("Could not rebuild derived race standing state for "+race.key(),e);}}
	private static void rebuildBioOpinion(Race race)throws Exception{Object bio=get(field(Race.class,"bio"),race);if(bio==null)return;Field improve=field(bio.getClass(),"improve");Object old=get(improve,bio);Object datas=get(field(old.getClass(),"datas"),old);Constructor<?> constructor=old.getClass().getDeclaredConstructors()[0];constructor.setAccessible(true);set(improve,bio,constructor.newInstance(datas,race));}
	private static void rebuildMostHated(Race race,java.util.Collection<Race> all){double[] values=(double[])get(field(RacePreferrence.class,"others"),race.pref());Race hated=race;double min=Double.MAX_VALUE;for(Race other:all)if(other!=race&&values[other.index()]<min){min=values[other.index()];hated=other;}set(field(RacePreferrence.class,"mostHated"),race.pref(),hated);}
	private static void rebuildResthomes(Race race){double[] work=(double[])get(field(RacePreferrence.class,"work"),race.pref());ArrayList<ROOM_RESTHOME> values=new ArrayList<>();for(ROOM_RESTHOME h:SETT.ROOMS().RESTHOMES)if(work[h.employment().eindex()]>0)values.add(h);values.sort((a,b)->Double.compare(work[a.employment().eindex()],work[b.employment().eindex()]));set(field(RacePreferrence.class,"resthomes"),race.pref(),new snake2d.util.sets.ArrayList<ROOM_RESTHOME>(values.toArray(new ROOM_RESTHOME[0])));}

	private static Map<String,Race> races(){HashMap<String,Race> m=new HashMap<>();for(Race r:RACES.all())if(m.put(r.key(),r)!=null)throw new IllegalStateException("Duplicate live race ID: "+r.key());return m;}
	private static STAT stat(String id){for(STAT s:STATS.all())if(s.key().equals(id))return s;return null;}
	private static HCLASS hclass(String id){for(HCLASS h:HCLASSES.ALL())if(h.key().equals(id))return h;return null;}
	private static ROOM_POOL pool(String id){for(RoomBlueprint b:SETT.ROOMS().all())if(b.key().equals(id)&&b instanceof ROOM_POOL)return(ROOM_POOL)b;return null;}
	private static RoomEmploymentSimple employment(String id){for(RoomBlueprint b:SETT.ROOMS().all())if(b.key().equals(id)&&b instanceof RoomBlueprintIns<?> i&&i.employment()!=null)return i.employment();return null;}
	private static Floor floor(String id){for(Floor f:SETT.FLOOR().all())if(f.key().equals(id))return f;return null;}
	private static CRIMES.CRIME crime(String id){for(CRIMES.CRIME c:CRIMES.ALL())if(c.key.equals(id))return c;return null;}
	private static CRIME_PUNISHMENTS.PUNISHMENT punishment(String id){for(CRIME_PUNISHMENTS.PUNISHMENT p:CRIME_PUNISHMENTS.ALL())if(p.key.equals(id))return p;return null;}
	private static RESOURCE resource(String id){for(RESOURCE r:RESOURCES.ALL())if(V7144ResourceStableIds.canonical(r.key(),RESOURCES.map().tryGet(V7144ResourceStableIds.publicAliasCandidate(r.key()))==r).equals(id))return r;return null;}
	private static boolean requiredText(TextTargetPlan p){for(RaceTextPatch x:p.patches)if(x.missingRacePolicy()==RaceMissingTargetPolicy.FAIL)return true;return false;}
	private static boolean requiredNumeric(NumericTargetPlan p){for(RaceNumericPatch x:p.patches)if(x.missingTargetPolicy()==RaceMissingTargetPolicy.FAIL)return true;return false;}
	private static boolean requiredRelationship(RelationshipTargetPlan p){for(RaceRelationshipPatch x:p.patches)if(x.missingTargetPolicy()==RaceMissingTargetPolicy.FAIL)return true;return false;}
	private static boolean requiredStanding(StandingTargetPlan p){for(RaceStandingPatch x:p.patches)if(x.missingTargetPolicy()==RaceMissingTargetPolicy.FAIL)return true;return false;}
	private static RuntimeException missing(String target){return new IllegalStateException("Required race attribute target is missing: "+target);}
	private static void skipped(String target,String reason){ChoirDiagnostics.info("RACE attribute-skipped target="+target+" reason="+reason);}
	private static List<String> textContributors(TextTargetPlan p){ArrayList<String>r=new ArrayList<>();for(RaceTextPatch x:p.patches)r.add(x.providerId()+"/"+x.patchId()+"="+x.operation()+"@"+x.priority());return r;}
	private static List<String> numericContributors(NumericTargetPlan p){ArrayList<String>r=new ArrayList<>();for(RaceNumericPatch x:p.patches)r.add(x.providerId()+"/"+x.patchId()+"="+x.operation()+x.value()+"@"+x.priority());return r;}
	private static List<String> relationshipContributors(RelationshipTargetPlan p){ArrayList<String>r=new ArrayList<>();for(RaceRelationshipPatch x:p.patches)r.add(x.providerId()+"/"+x.patchId()+"="+x.operation()+x.value()+"@"+x.priority());return r;}
	private static List<String> standingContributors(StandingTargetPlan p){ArrayList<String>r=new ArrayList<>();for(RaceStandingPatch x:p.patches)r.add(x.providerId()+"/"+x.patchId()+"="+x.operation()+x.value()+"/"+x.polarity()+"@"+x.priority());return r;}
	private static Field field(Class<?> type,String name){String key=type.getName()+'#'+name;Field f=fields.get(key);if(f!=null)return f;try{f=type.getDeclaredField(name);f.setAccessible(true);fields.put(key,f);return f;}catch(Exception e){throw new IllegalStateException("Required exact-v71.44 field is unavailable: "+key,e);}}
	private static Object get(Field f,Object owner){try{return f.get(owner);}catch(Exception e){throw new IllegalStateException("Could not read "+f,e);}}
	private static void set(Field f,Object owner,Object value){try{f.set(owner,value);}catch(Exception e){throw new IllegalStateException("Could not write "+f,e);}}
	static synchronized void disposed(){lastGeneration=0;lastRegistryIdentity=0;}
}
