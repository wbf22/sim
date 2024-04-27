package bear.game.company.environment;

import java.util.Map;
import java.util.Random;

import bear.game.company.environment.opportunities.Opportunity;

public class Person {
    // 0-10 for the following attributes
    public Integer intelligence;
    public Integer greed;
    public Map<Opportunity, Integer> skill;
    public Integer communication;
    public Integer likeability;
    public Integer honesty;
    public Integer tendancyToFollowCrowd;
    public Integer efficiency;
    public Integer salary = 1000;

    public Integer currentMotivation = 1000;
    
    private Random random = new Random();
    
    // game state
    public Activity<?> currentActivity;


    public Person() {
        intelligence = random.nextInt(11);
        greed = random.nextInt(11);
        communication = random.nextInt(11);
        likeability = random.nextInt(11);
        honesty = random.nextInt(11);
        tendancyToFollowCrowd = random.nextInt(11);
        efficiency = random.nextInt(11);
    }


    public void updateRandomAttribute() {
        int attributeToUpdate = random.nextInt(7);

        switch (attributeToUpdate) {
            case 0:
                intelligence += random.nextInt(-1, 2);
                if (intelligence < 0) intelligence = 0;
                if (intelligence > 10) intelligence = 10;
                break;
            case 1:
                greed += random.nextInt(-1, 2);
                if (greed < 0) greed = 0;
                if (greed > 10) greed = 10;
                break;
            case 2:
                communication += random.nextInt(-1, 2);
                if (communication < 0) communication = 0;
                if (communication > 10) communication = 10;
                break;
            case 3:
                likeability += random.nextInt(-1, 2);
                if (likeability < 0) likeability = 0;
                if (likeability > 10) likeability = 10;
                break;
            case 4:
                honesty += random.nextInt(-1, 2);
                if (honesty < 0) honesty = 0;
                if (honesty > 10) honesty = 10;
                break;
            case 5:
                tendancyToFollowCrowd += random.nextInt(-1, 2);
                if (tendancyToFollowCrowd < 0) tendancyToFollowCrowd = 0;
                if (tendancyToFollowCrowd > 10) tendancyToFollowCrowd = 10;
                break;
            case 6:
                efficiency += random.nextInt(-1, 2);
                if (efficiency < 0) efficiency = 0;
                if (efficiency > 10) efficiency = 10;
                break;
        }
    }

    
    public Integer getDailyWorkAccomplished(Activity<?> activity) {
        Integer skillForActivity = skill.get(activity.opportunity);
        return skillForActivity * efficiency * intelligence;
    }


    public Integer getSkillTotal() {
        return skill.values().stream()
            .reduce(0, Integer::sum);
    }
}
