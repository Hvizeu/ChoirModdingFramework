package choir.internal.race;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;

import choir.api.race.RaceCollectionOperation;
import choir.api.race.RaceNumericOperation;
import choir.api.race.RaceNumericPatch;
import choir.api.race.RaceRegistrationResult;
import choir.api.race.RaceRelationshipPatch;
import choir.api.race.RaceStandingPatch;
import choir.api.race.RaceStandingPolarity;
import choir.api.race.RaceTextPatch;
import choir.internal.ChoirDiagnostics;
import choir.internal.platform.PlatformRuntime;

/** Process-retained, game-type-free race attribute descriptors and deterministic composition. */
public final class RaceAttributeRegistry {
	private static final Map<String, RaceTextPatch> text = new TreeMap<String, RaceTextPatch>();
	private static final Map<String, RaceNumericPatch> numeric = new TreeMap<String, RaceNumericPatch>();
	private static final Map<String, RaceRelationshipPatch> relationships = new TreeMap<String, RaceRelationshipPatch>();
	private static final Map<String, RaceStandingPatch> standings = new TreeMap<String, RaceStandingPatch>();
	private static boolean adapterReady;
	private RaceAttributeRegistry() { }

	public static synchronized RaceRegistrationResult register(RaceTextPatch patch) {
		if (patch == null) throw new IllegalArgumentException("Race text patch is required.");
		return put(text, identity(patch.providerId(), patch.patchId()), patch, equivalentText(patch), "text", patch.raceId() + ':' + patch.field());
	}
	public static synchronized RaceRegistrationResult register(RaceNumericPatch patch) {
		if (patch == null) throw new IllegalArgumentException("Race numeric patch is required.");
		return put(numeric, identity(patch.providerId(), patch.patchId()), patch, equivalentNumeric(patch), "numeric", numericTarget(patch));
	}
	public static synchronized RaceRegistrationResult register(RaceRelationshipPatch patch) {
		if (patch == null) throw new IllegalArgumentException("Race relationship patch is required.");
		return put(relationships, identity(patch.providerId(), patch.patchId()), patch, equivalentRelationship(patch), "relationship", relationshipTarget(patch));
	}
	public static synchronized RaceRegistrationResult register(RaceStandingPatch patch) {
		if (patch == null) throw new IllegalArgumentException("Race standing patch is required.");
		return put(standings, identity(patch.providerId(), patch.patchId()), patch, equivalentStanding(patch), "standing", standingTarget(patch));
	}

	private static <T> RaceRegistrationResult put(Map<String,T> map, String id, T value, boolean equivalent,
			String type, String target) {
		T old = map.get(id); RaceRegistrationResult result;
		if (old == null) { map.put(id, value); result = RaceRegistrationResult.ACCEPTED; }
		else result = equivalent ? RaceRegistrationResult.IDEMPOTENT : RaceRegistrationResult.REJECTED_CONFLICT;
		ChoirDiagnostics.info("RACE attribute-registration type=" + type + " identity=" + id.replace('\u0000', '/')
				+ " target=" + target + " result=" + result);
		return result;
	}

	public static synchronized AttributePlan plan() {
		for (RaceTextPatch p : text.values()) requireProvider(p.providerId());
		for (RaceNumericPatch p : numeric.values()) requireProvider(p.providerId());
		for (RaceRelationshipPatch p : relationships.values()) requireProvider(p.providerId());
		for (RaceStandingPatch p : standings.values()) requireProvider(p.providerId());
		return new AttributePlan(groupText(), groupNumeric(), groupRelationships(), groupStandings());
	}

	public static TextResolution resolve(TextTargetPlan plan, List<String> base) {
		LinkedHashSet<String> effective = new LinkedHashSet<String>(base);
		for (Map.Entry<Integer,ArrayList<RaceTextPatch>> level : byPriority(plan.patches).entrySet()) {
			ArrayList<RaceTextPatch> replacements = new ArrayList<RaceTextPatch>();
			TreeSet<String> removes = new TreeSet<String>();
			ArrayList<String> prepends = new ArrayList<String>(), appends = new ArrayList<String>();
			TreeMap<String,TreeSet<RaceCollectionOperation>> operations = new TreeMap<String,TreeSet<RaceCollectionOperation>>();
			for (RaceTextPatch patch : level.getValue()) {
				if (patch.operation() == RaceCollectionOperation.REPLACE) replacements.add(patch);
				for (String value : patch.values()) operations.computeIfAbsent(value, ignored -> new TreeSet<RaceCollectionOperation>()).add(patch.operation());
			}
			if (replacements.size() > 1) throw new IllegalStateException("Conflicting race text replacements target=" + plan.targetId + " priority=" + level.getKey());
			for (Map.Entry<String,TreeSet<RaceCollectionOperation>> operation : operations.entrySet())
				if (operation.getValue().contains(RaceCollectionOperation.REMOVE)
						&& (operation.getValue().contains(RaceCollectionOperation.APPEND) || operation.getValue().contains(RaceCollectionOperation.PREPEND)))
					throw new IllegalStateException("Conflicting race text add/remove target=" + plan.targetId + " priority=" + level.getKey());
			if (!replacements.isEmpty()) { effective.clear(); effective.addAll(replacements.get(0).values()); }
			for (RaceTextPatch patch : level.getValue()) {
				if (patch.operation() == RaceCollectionOperation.REMOVE) removes.addAll(patch.values());
				if (patch.operation() == RaceCollectionOperation.PREPEND) prepends.addAll(patch.values());
				if (patch.operation() == RaceCollectionOperation.APPEND) appends.addAll(patch.values());
			}
			effective.removeAll(removes);
			LinkedHashSet<String> merged = new LinkedHashSet<String>(); merged.addAll(prepends); merged.addAll(effective); merged.addAll(appends); effective = merged;
		}
		return new TextResolution(plan.targetId, List.copyOf(effective));
	}

	public static NumericResolution resolve(NumericTargetPlan plan, double base) {
		double value = base;
		for (Map.Entry<Integer,ArrayList<RaceNumericPatch>> level : byPriorityNumeric(plan.patches).entrySet()) {
			ArrayList<RaceNumericPatch> replacements = new ArrayList<RaceNumericPatch>(); double add = 0.0, multiply = 1.0;
			for (RaceNumericPatch patch : level.getValue()) {
				if (patch.operation() == RaceNumericOperation.REPLACE) replacements.add(patch);
				else if (patch.operation() == RaceNumericOperation.ADD) add += patch.value();
				else multiply *= patch.value();
			}
			if (replacements.size() > 1) throw new IllegalStateException("Conflicting race numeric replacements target=" + plan.targetId + " priority=" + level.getKey());
			if (!replacements.isEmpty()) value = replacements.get(0).value();
			value = (value + add) * multiply;
		}
		value = Math.max(plan.patches.get(0).attribute().minimum(), Math.min(plan.patches.get(0).attribute().maximum(), value));
		return new NumericResolution(plan.targetId, value);
	}

	public static RelationshipResolution resolve(RelationshipTargetPlan plan, double base) {
		double value = resolveNumber(plan.targetId, base, plan.patches); return new RelationshipResolution(plan.targetId, clamp01(value));
	}
	public static StandingResolution resolve(StandingTargetPlan plan, double base, boolean inverted) {
		double value = resolveStandingNumber(plan.targetId, base, plan.patches); RaceStandingPolarity polarity = RaceStandingPolarity.KEEP;
		TreeMap<Integer,ArrayList<RaceStandingPatch>> by = byPriorityStanding(plan.patches);
		for (Map.Entry<Integer,ArrayList<RaceStandingPatch>> level : by.entrySet()) {
			TreeSet<RaceStandingPolarity> values = new TreeSet<RaceStandingPolarity>();
			for (RaceStandingPatch patch : level.getValue()) if (patch.polarity() != RaceStandingPolarity.KEEP) values.add(patch.polarity());
			if (values.size() > 1) throw new IllegalStateException("Conflicting race standing polarities target=" + plan.targetId + " priority=" + level.getKey());
			if (!values.isEmpty()) polarity = values.first();
		}
		boolean effectiveInverted = polarity == RaceStandingPolarity.KEEP ? inverted : polarity == RaceStandingPolarity.DISLIKE;
		return new StandingResolution(plan.targetId, Math.max(0.0, Math.min(1_000_000.0, value)), effectiveInverted);
	}

	private static double resolveNumber(String target, double base, List<RaceRelationshipPatch> patches) {
		double value = base;
		for (Map.Entry<Integer,ArrayList<RaceRelationshipPatch>> level : byPriorityRelationship(patches).entrySet()) {
			ArrayList<RaceRelationshipPatch> replacements = new ArrayList<RaceRelationshipPatch>(); double add=0,multiply=1;
			for (RaceRelationshipPatch patch : level.getValue()) { if (patch.operation()==RaceNumericOperation.REPLACE) replacements.add(patch); else if (patch.operation()==RaceNumericOperation.ADD) add+=patch.value(); else multiply*=patch.value(); }
			if (replacements.size()>1) throw new IllegalStateException("Conflicting race relationship replacements target="+target+" priority="+level.getKey());
			if (!replacements.isEmpty()) value=replacements.get(0).value(); value=(value+add)*multiply;
		}
		return value;
	}
	private static double resolveStandingNumber(String target, double base, List<RaceStandingPatch> patches) {
		double value=base;
		for (Map.Entry<Integer,ArrayList<RaceStandingPatch>> level : byPriorityStanding(patches).entrySet()) {
			ArrayList<RaceStandingPatch> replacements=new ArrayList<RaceStandingPatch>(); double add=0,multiply=1;
			for (RaceStandingPatch patch:level.getValue()) { if(patch.operation()==RaceNumericOperation.REPLACE) replacements.add(patch); else if(patch.operation()==RaceNumericOperation.ADD) add+=patch.value(); else multiply*=patch.value(); }
			if(replacements.size()>1) throw new IllegalStateException("Conflicting race standing replacements target="+target+" priority="+level.getKey());
			if(!replacements.isEmpty()) value=replacements.get(0).value(); value=(value+add)*multiply;
		}
		return value;
	}

	private static List<TextTargetPlan> groupText() { TreeMap<String,ArrayList<RaceTextPatch>> g=new TreeMap<>(); for(RaceTextPatch p:text.values())g.computeIfAbsent(textTarget(p),x->new ArrayList<>()).add(p); ArrayList<TextTargetPlan> r=new ArrayList<>(); for(var e:g.entrySet()){e.getValue().sort(textOrder()); RaceTextPatch f=e.getValue().get(0); r.add(new TextTargetPlan(e.getKey(),f.raceId(),f.field(),e.getValue()));} return List.copyOf(r); }
	private static List<NumericTargetPlan> groupNumeric() { TreeMap<String,ArrayList<RaceNumericPatch>> g=new TreeMap<>(); for(RaceNumericPatch p:numeric.values())g.computeIfAbsent(numericTarget(p),x->new ArrayList<>()).add(p); ArrayList<NumericTargetPlan> r=new ArrayList<>(); for(var e:g.entrySet()){e.getValue().sort(numericOrder()); RaceNumericPatch f=e.getValue().get(0); r.add(new NumericTargetPlan(e.getKey(),f.raceId(),f.attribute(),f.subjectId(),e.getValue()));} return List.copyOf(r); }
	private static List<RelationshipTargetPlan> groupRelationships(){TreeMap<String,ArrayList<RaceRelationshipPatch>>g=new TreeMap<>();for(RaceRelationshipPatch p:relationships.values())g.computeIfAbsent(relationshipTarget(p),x->new ArrayList<>()).add(p);ArrayList<RelationshipTargetPlan>r=new ArrayList<>();for(var e:g.entrySet()){e.getValue().sort(relationshipOrder());RaceRelationshipPatch f=e.getValue().get(0);r.add(new RelationshipTargetPlan(e.getKey(),f.raceId(),f.otherRaceId(),e.getValue()));}return List.copyOf(r);}
	private static List<StandingTargetPlan> groupStandings(){TreeMap<String,ArrayList<RaceStandingPatch>>g=new TreeMap<>();for(RaceStandingPatch p:standings.values())g.computeIfAbsent(standingTarget(p),x->new ArrayList<>()).add(p);ArrayList<StandingTargetPlan>r=new ArrayList<>();for(var e:g.entrySet()){e.getValue().sort(standingOrder());RaceStandingPatch f=e.getValue().get(0);r.add(new StandingTargetPlan(e.getKey(),f.raceId(),f.statId(),f.humanoidClassId(),e.getValue()));}return List.copyOf(r);}
	private static TreeMap<Integer,ArrayList<RaceTextPatch>> byPriority(List<RaceTextPatch>p){TreeMap<Integer,ArrayList<RaceTextPatch>>r=new TreeMap<>();for(RaceTextPatch x:p)r.computeIfAbsent(x.priority(),y->new ArrayList<>()).add(x);return r;}
	private static TreeMap<Integer,ArrayList<RaceNumericPatch>> byPriorityNumeric(List<RaceNumericPatch>p){TreeMap<Integer,ArrayList<RaceNumericPatch>>r=new TreeMap<>();for(RaceNumericPatch x:p)r.computeIfAbsent(x.priority(),y->new ArrayList<>()).add(x);return r;}
	private static TreeMap<Integer,ArrayList<RaceRelationshipPatch>> byPriorityRelationship(List<RaceRelationshipPatch>p){TreeMap<Integer,ArrayList<RaceRelationshipPatch>>r=new TreeMap<>();for(RaceRelationshipPatch x:p)r.computeIfAbsent(x.priority(),y->new ArrayList<>()).add(x);return r;}
	private static TreeMap<Integer,ArrayList<RaceStandingPatch>> byPriorityStanding(List<RaceStandingPatch>p){TreeMap<Integer,ArrayList<RaceStandingPatch>>r=new TreeMap<>();for(RaceStandingPatch x:p)r.computeIfAbsent(x.priority(),y->new ArrayList<>()).add(x);return r;}
	private static Comparator<RaceTextPatch> textOrder(){return Comparator.comparingInt(RaceTextPatch::priority).thenComparing(RaceTextPatch::providerId).thenComparing(RaceTextPatch::patchId);}
	private static Comparator<RaceNumericPatch> numericOrder(){return Comparator.comparingInt(RaceNumericPatch::priority).thenComparing(RaceNumericPatch::providerId).thenComparing(RaceNumericPatch::patchId);}
	private static Comparator<RaceRelationshipPatch> relationshipOrder(){return Comparator.comparingInt(RaceRelationshipPatch::priority).thenComparing(RaceRelationshipPatch::providerId).thenComparing(RaceRelationshipPatch::patchId);}
	private static Comparator<RaceStandingPatch> standingOrder(){return Comparator.comparingInt(RaceStandingPatch::priority).thenComparing(RaceStandingPatch::providerId).thenComparing(RaceStandingPatch::patchId);}
	private static String identity(String provider,String patch){return provider+'\u0000'+patch;}
	private static String textTarget(RaceTextPatch p){return "choir.race.text:"+p.raceId()+':'+p.field();}
	private static String numericTarget(RaceNumericPatch p){return "choir.race.numeric:"+p.raceId()+':'+p.attribute()+':'+p.subjectId();}
	private static String relationshipTarget(RaceRelationshipPatch p){return "choir.race.relationship:"+p.raceId()+':'+p.otherRaceId();}
	private static String standingTarget(RaceStandingPatch p){return "choir.race.standing:"+p.raceId()+':'+p.statId()+':'+p.humanoidClassId();}
	private static void requireProvider(String provider){if(!PlatformRuntime.isActive(provider))throw new IllegalStateException("Race attribute provider is not active in the resolved platform graph: "+provider);}
	private static double clamp01(double v){return Math.max(0.0,Math.min(1.0,v));}
	private static boolean equivalentText(RaceTextPatch p){RaceTextPatch o=text.get(identity(p.providerId(),p.patchId()));return o!=null&&o.raceId().equals(p.raceId())&&o.field()==p.field()&&o.operation()==p.operation()&&o.priority()==p.priority()&&o.values().equals(p.values())&&o.missingRacePolicy()==p.missingRacePolicy();}
	private static boolean equivalentNumeric(RaceNumericPatch p){RaceNumericPatch o=numeric.get(identity(p.providerId(),p.patchId()));return o!=null&&o.raceId().equals(p.raceId())&&o.attribute()==p.attribute()&&o.subjectId().equals(p.subjectId())&&o.operation()==p.operation()&&o.priority()==p.priority()&&Double.compare(o.value(),p.value())==0&&o.missingTargetPolicy()==p.missingTargetPolicy();}
	private static boolean equivalentRelationship(RaceRelationshipPatch p){RaceRelationshipPatch o=relationships.get(identity(p.providerId(),p.patchId()));return o!=null&&o.raceId().equals(p.raceId())&&o.otherRaceId().equals(p.otherRaceId())&&o.operation()==p.operation()&&o.priority()==p.priority()&&Double.compare(o.value(),p.value())==0&&o.missingTargetPolicy()==p.missingTargetPolicy();}
	private static boolean equivalentStanding(RaceStandingPatch p){RaceStandingPatch o=standings.get(identity(p.providerId(),p.patchId()));return o!=null&&o.raceId().equals(p.raceId())&&o.statId().equals(p.statId())&&o.humanoidClassId().equals(p.humanoidClassId())&&o.operation()==p.operation()&&o.priority()==p.priority()&&Double.compare(o.value(),p.value())==0&&o.polarity()==p.polarity()&&o.missingTargetPolicy()==p.missingTargetPolicy();}
	public static synchronized void adapterReady(boolean ready){adapterReady=ready;}
	public static synchronized boolean capabilityReady(){return adapterReady;}
	static synchronized void resetForTests(){text.clear();numeric.clear();relationships.clear();standings.clear();adapterReady=false;}

	public static final class AttributePlan { public final List<TextTargetPlan> text; public final List<NumericTargetPlan> numeric; public final List<RelationshipTargetPlan> relationships; public final List<StandingTargetPlan> standings; AttributePlan(List<TextTargetPlan>t,List<NumericTargetPlan>n,List<RelationshipTargetPlan>r,List<StandingTargetPlan>s){text=t;numeric=n;relationships=r;standings=s;} public boolean isEmpty(){return text.isEmpty()&&numeric.isEmpty()&&relationships.isEmpty()&&standings.isEmpty();} }
	public static final class TextTargetPlan { public final String targetId,raceId; public final choir.api.race.RaceTextField field; public final List<RaceTextPatch> patches; TextTargetPlan(String t,String r,choir.api.race.RaceTextField f,List<RaceTextPatch>p){targetId=t;raceId=r;field=f;patches=List.copyOf(p);} }
	public static final class NumericTargetPlan { public final String targetId,raceId,subjectId; public final choir.api.race.RaceNumericAttribute attribute; public final List<RaceNumericPatch> patches; NumericTargetPlan(String t,String r,choir.api.race.RaceNumericAttribute a,String s,List<RaceNumericPatch>p){targetId=t;raceId=r;attribute=a;subjectId=s;patches=List.copyOf(p);} }
	public static final class RelationshipTargetPlan { public final String targetId,raceId,otherRaceId; public final List<RaceRelationshipPatch> patches; RelationshipTargetPlan(String t,String r,String o,List<RaceRelationshipPatch>p){targetId=t;raceId=r;otherRaceId=o;patches=List.copyOf(p);} }
	public static final class StandingTargetPlan { public final String targetId,raceId,statId,humanoidClassId; public final List<RaceStandingPatch> patches; StandingTargetPlan(String t,String r,String s,String h,List<RaceStandingPatch>p){targetId=t;raceId=r;statId=s;humanoidClassId=h;patches=List.copyOf(p);} }
	public static final class TextResolution { public final String targetId; public final List<String> values; TextResolution(String t,List<String>v){targetId=t;values=v;} }
	public static final class NumericResolution { public final String targetId; public final double value; NumericResolution(String t,double v){targetId=t;value=v;} }
	public static final class RelationshipResolution { public final String targetId; public final double value; RelationshipResolution(String t,double v){targetId=t;value=v;} }
	public static final class StandingResolution { public final String targetId; public final double value; public final boolean inverted; StandingResolution(String t,double v,boolean i){targetId=t;value=v;inverted=i;} }
}
