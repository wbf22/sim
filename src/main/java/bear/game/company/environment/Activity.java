package bear.game.company.environment;

import java.util.List;
import java.util.Random;

import bear.game.company.environment.opportunities.ContractProduct;
import bear.game.company.environment.opportunities.Offer;
import bear.game.company.environment.opportunities.Opportunity;
import bear.game.company.environment.opportunities.Product;
import bear.game.company.strategies.CompanyStrategy;

public class Activity<T> {

    public ActivityType type;
    public Opportunity opportunity;
    public T result;
    public Integer workLeftToDo;
    public Boolean isApproved;
    public List<Person> peopleWorkingOnActivity;

    public Random random = new Random();


    public void setTimeToComplete(Integer decisionMakingTime, Integer difficulty) {
        workLeftToDo = decisionMakingTime * difficulty / 5;
        workLeftToDo += difficulty;
    }


    public void determineResult(Person avgLeader, List<Person> membersWorkingOnActivity, Integer value, CompanyStrategy company) {
        

        Integer skillPoints = membersWorkingOnActivity.stream()
            .mapToInt(member -> {
                Integer skillPts = member.skill.get(opportunity);
                return skillPts == null ? 0 : skillPts;
            })
            .sum();

        switch (type) {
            case BID_FOR_CONTRACT:
                Offer offer = new Offer();
                offer.price = opportunity.value * opportunity.complexity / skillPoints;
                offer.price *= avgLeader.greed;

                offer.timeToComplete = workLeftToDo;
                offer.timeToComplete /= avgLeader.honesty;

                offer.opportunity = opportunity;
                
                result = (T) offer;
                break;

            case RESEARCH:
                // determine if research will be cancelled by greedy or dumb leader
                // (sometimes it's good to cancel research but for the simulation hopefully that's covered by the intial choice to research)
                if (avgLeader.greed - avgLeader.intelligence > random.nextInt(5, 10)) {
                    result = null;
                    membersWorkingOnActivity.forEach(member -> {
                        member.currentActivity = null;
                    });
                }

                Product product = new Product();
                product.quality = skillPoints * random.nextInt(1, 3);
                product.quality *= avgLeader.intelligence / 5;
                product.price = product.quality * opportunity.value;

                result = (T) product;
                break;
            case ADVERTISING_AND_SALES:
                
                break;
            case CONTRACT_PRODUCT_DEVELOPMENT:

                ContractProduct contractProduct = new ContractProduct();
                contractProduct.quality = skillPoints * random.nextInt(1, 3);
                contractProduct.quality *= avgLeader.intelligence / 5;
                contractProduct.price = contractProduct.quality * opportunity.value;

                result = (T) contractProduct;
                break;
            case HIRE_PERSON:

                Person newPerson = new Person();
                float mult = company.reputation / avgLeader.likeability;
                newPerson.intelligence = Math.round(newPerson.intelligence * mult);
                if (newPerson.intelligence > 10) newPerson.intelligence = 10;
                newPerson.efficiency = Math.round(newPerson.efficiency * mult);
                if (newPerson.efficiency > 10) newPerson.efficiency = 10;
                
                break;
            case PRODUCT_IMPROVEMENT:
                
                

                break;
                
            default:
                break;
        }


    }


}
