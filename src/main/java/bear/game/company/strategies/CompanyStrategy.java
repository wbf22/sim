package bear.game.company.strategies;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.function.BinaryOperator;
import java.util.Objects;

import bear.game.company.environment.Activity;
import bear.game.company.environment.ActivityType;
import bear.game.company.environment.Person;
import bear.game.company.environment.equipment.Equipment;
import bear.game.company.environment.opportunities.ContractProduct;
import bear.game.company.environment.opportunities.Offer;
import bear.game.company.environment.opportunities.Opportunity;
import bear.game.company.environment.opportunities.Product;

public abstract class CompanyStrategy {

    public Integer reputation;
    public Integer money;
    public Integer debt;

    public List<Person> members;
    public List<Equipment> equipment;

    public List<Opportunity> contracts;
    public List<Product> currentProducts;
    public List<Offer> offers;
    public List<Activity> activities;

    private Random random = new Random();
    private static Integer MAX_SKILL = 100;


    BinaryOperator<Person> leaderAverager = (person1, person2) -> {
        if (person1 == null) return person2;
        Person leader = new Person();
        leader.intelligence = (person1.intelligence + person2.intelligence) / 2;
        leader.greed = (person1.greed + person2.greed) / 2;
        leader.communication = (person1.communication + person2.communication) / 2;
        leader.likeability = (person1.likeability + person2.likeability) / 2;
        leader.honesty = (person1.honesty + person2.honesty) / 2;
        leader.tendancyToFollowCrowd = (person1.tendancyToFollowCrowd + person2.tendancyToFollowCrowd) / 2;
        leader.efficiency = (person1.efficiency + person2.efficiency) / 2;

        return leader;
    };




    public abstract List<Person> getLeaders(Person person);


    public abstract List<Person> getMembersMakingDecision(Activity<?> activity);



    /**
     * Decide which members should work on the activity
     * @return
     */
    public List<Person> delegateMembersToActivity(Activity<?> activity) {
        
        // decide on wether to do activity
        Integer howBusyWorkerAre = getHireNeed();
        List<Person> membersMakingDecision = getMembersMakingDecision(activity);
        Person avgOfLeaders = membersMakingDecision.stream()
            .reduce(null, leaderAverager);
        Integer decisionMakingTime = membersMakingDecision.stream()
            .mapToInt(member -> 10 - member.efficiency)
            .reduce(0, Integer::sum);
        if (activity.isApproved == null) {
            
            // queue decision making for people involved
            Activity<Void> decisionMakingActivity = new Activity<>();
            decisionMakingActivity.type = ActivityType.DECIDE_TO_ACCEPT_OTHER_ACTIVITY;
            decisionMakingActivity.setTimeToComplete(decisionMakingTime, 5);

            // determine best decision
            boolean shouldDoActivity = true;

            switch (activity.type) {
                case HIRE_PERSON:
                    Integer hireValue = howBusyWorkerAre;
                    shouldDoActivity = hireValue > 0;
                    break;
                default:
                    Integer value = getActivityValue(howBusyWorkerAre, avgOfLeaders, activity);
                    shouldDoActivity = value > 0;
                    break;
            }

            // determine if leaders make the best decision
            boolean leadersMakeBestDecision = (random.nextInt(11) * avgOfLeaders.intelligence / 10) > 5;
            activity.isApproved = (shouldDoActivity && leadersMakeBestDecision)? shouldDoActivity : !shouldDoActivity;
            
        }

        List<Person> membersWorkingOnActivity = new ArrayList<>();
        if (activity.isApproved == true) {

            // queue up activity work strategically in members
            Integer neededMembers = getActivityValue(howBusyWorkerAre, avgOfLeaders, activity) / 10;
            Map<Integer, Person> memberSkillForActivity = new HashMap<>();
            for (Person member : members) {
                if (member.currentActivity == null) {
                    Integer skill = member.skill.get(activity.opportunity);
                    skill = (skill == null)? 0 : skill;
                    memberSkillForActivity.put(skill, member);
                }
            }
            membersWorkingOnActivity = memberSkillForActivity.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(Map.Entry::getValue)
                .limit(neededMembers)
                .toList();
                
            activity.setTimeToComplete(decisionMakingTime, activity.opportunity.complexity);
            membersWorkingOnActivity.stream()
                .forEach(member -> {
                    member.currentActivity = activity;
                });

            // determine activity outcome
            // activity.determineResult(membersWorkingOnActivity);


        }

        return membersWorkingOnActivity;
    }

    public Integer getActivityValue(Integer howBusyWorkerAre, Person avgOfLeaders, Activity<?> activity) {
        Integer value = activity.opportunity.value;
        value -= howBusyWorkerAre / (avgOfLeaders.greed);
        value /= money;

        return value;
    }


    public DailyOutput performDailyTasks(List<Opportunity> openMarketOpportunities, List<Opportunity> contractsForBid) {

        // decide to bid for contracts
        contractsForBid.stream()
            .forEach(contract -> {
                Activity<Offer> activity = new Activity<>();
                activity.opportunity = contract;
                activity.type = ActivityType.BID_FOR_CONTRACT;

                this.delegateMembersToActivity(activity).stream()
                    .forEach(person -> {
                        person.currentActivity = activity;
                    });
            });


        // prioritize work on existing contracts
        contracts.stream()
            .forEach(contract -> {
                Optional<Activity> activityOpt = activities.stream()
                    .filter(act -> act.opportunity == contract)
                    .findFirst();
                
                Activity<ContractProduct> activity;
                if (!activityOpt.isPresent()) {
                    activity = new Activity<>();
                    activity.opportunity = contract;
                    activity.type = ActivityType.CONTRACT_PRODUCT_DEVELOPMENT;

                    activities.add(activity);
                }
                else {
                    activity = activityOpt.get();
                }


                delegateMembersToActivity(activity).stream()
                    .forEach(person -> {
                        person.currentActivity = activity;
                    });

                
            });


        // research and match new products to market opportunities
        openMarketOpportunities.stream()
            .forEach(opportunity -> {
                Activity<Product> activity = new Activity<>();
                activity.opportunity = opportunity;
                activity.type = ActivityType.RESEARCH;

                delegateMembersToActivity(activity).stream()
                    .forEach(person -> {
                        person.currentActivity = activity;
                    });
            });


        // improve existing products
        currentProducts.stream()
            .forEach(productToImprove -> {
                Activity<Product> activity = new Activity<>();
                activity.opportunity = productToImprove.opportunity;
                activity.type = ActivityType.PRODUCT_IMPROVEMENT;

                delegateMembersToActivity(activity).stream()
                    .forEach(person -> {
                        person.currentActivity = activity;
                    });
            });


        // retrieve daily output from people working on activities
        DailyOutput output = new DailyOutput();
        List<Activity> finishedActivities = members.stream()
            .map(member -> {
                Activity<?> activity = member.currentActivity;
                if (activity.workLeftToDo <= 0) {
                    member.currentActivity = null;
                    return (Activity) activity;
                }

                // determine work accomplished
                Integer workAccomplished = member.getDailyWorkAccomplished(activity);
                Person leader = getLeaders(member).stream() // determine if leader impeads progress 
                    .reduce(member, leaderAverager);
                if (leader.intelligence < random.nextInt(0, 4)) workAccomplished /= 2;
                member.currentActivity.workLeftToDo -= workAccomplished;

                return null;
            })
            .filter(Objects::nonNull)
            .distinct()
            .toList();

        finishedActivities.stream()
            .forEach(activity -> {
                switch (activity.type) {
                    case BID_FOR_CONTRACT:
                        Offer offer = (Offer) activity.result;
                        offers.add(offer);
                        break;
                    case CONTRACT_PRODUCT_DEVELOPMENT:
                        Object result = activity.result;
                        if (result != null) {
                            ContractProduct product = (ContractProduct) result;
                            currentProducts.add(product);
                        }
                        break;
                    case RESEARCH:
                        Object researchResult = activity.result;
                        if (researchResult != null) {
                            Product product = (Product) researchResult;
                            currentProducts.add(product);
                        }
                        break;
                    default:
                        break;
                }
            });

        output.products = currentProducts;
        output.offers = offers;


        // update memebers statistics
        members.stream()
            .forEach(member -> {
                // motivation
                if (member.currentActivity == null) member.currentMotivation += 1;
                Person leader = getLeaders(member).stream()
                    .reduce(member, leaderAverager);
                if (leader.greed > 7) member.currentMotivation -= 1;
                if (leader.honesty < 4) member.currentMotivation -= 1;
                if (leader.likeability < 4) member.currentMotivation -= 1;
                member.currentMotivation += random.nextInt(-1, 2);


                // skill
                if (member.currentActivity != null) {
                    Integer skill = member.skill.get(member.currentActivity.opportunity);
                    if (skill < MAX_SKILL) member.skill.put(member.currentActivity.opportunity, skill + 1);
                }

            });
            

        // determine if some members quit and determine if all are busy
        members.stream()
            .forEach(member -> {
                if (member.currentMotivation < 0) {
                    members.remove(member);
                }
            });

        // try to hire if necessary
        Integer hireNeed = getHireNeed();

        if (hireNeed > 100) {
            Activity<Person> activity = new Activity<>();
            activity.type = ActivityType.HIRE_PERSON;
            delegateMembersToActivity(activity);
        }
        

        return output;
    }

    public Integer getHireNeed() {
        return members.stream()
            .mapToInt(member -> {
                if (member.currentActivity == null) {
                    return 0;
                }
                Integer memberDailyCapacity = member.getDailyWorkAccomplished(member.currentActivity);
                return member.currentActivity.workLeftToDo / memberDailyCapacity;
            })
            .reduce(0, Integer::sum);
    }

    public void yearlyTasks() {
        members.forEach(member -> {
            // update attributes (person growth or regression)
            member.updateRandomAttribute();

            // decide on giving raise or firing
            List<Person> leaders = getLeaders(member);
            Person leader = leaders.stream()
                .reduce(null, leaderAverager);
            Integer goingToGiveRaise = money / members.size();
            if (leaders.contains(member)) { // leaders may give themselves more raises
                goingToGiveRaise *= member.greed / leaders.size();
            }
            goingToGiveRaise /= leader.greed;
            goingToGiveRaise *= leader.honesty;
            goingToGiveRaise *= leader.likeability;
            goingToGiveRaise *= member.likeability;
            goingToGiveRaise *= member.honesty;
            goingToGiveRaise *= member.efficiency;
            goingToGiveRaise *= member.intelligence;
            goingToGiveRaise *= member.getSkillTotal();
            if (goingToGiveRaise > member.salary) {
                member.salary += 1000;
                member.currentMotivation += 20;
            }
            else if (goingToGiveRaise < member.salary) {
                members.remove(member);
            }

            // update attributes based on leaders
            member.greed += Math.round((leader.greed - member.greed) * member.tendancyToFollowCrowd / 100.0f );
            member.intelligence += Math.round((leader.intelligence - member.intelligence) * member.tendancyToFollowCrowd / 100.0f );
            member.efficiency += Math.round((leader.efficiency - member.efficiency) * member.tendancyToFollowCrowd / 100.0f );
            member.honesty += Math.round((leader.honesty - member.honesty) * member.tendancyToFollowCrowd / 100.0f );
            member.likeability += Math.round((leader.likeability - member.likeability) * member.tendancyToFollowCrowd / 100.0f );
            member.communication += Math.round((leader.communication - member.communication) * member.tendancyToFollowCrowd / 100.0f );


        });
    }
} 
