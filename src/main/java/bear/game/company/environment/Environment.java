package bear.game.company.environment;

import java.nio.file.OpenOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import bear.game.company.environment.opportunities.ContractProduct;
import bear.game.company.environment.opportunities.Offer;
import bear.game.company.environment.opportunities.Opportunity;
import bear.game.company.environment.opportunities.Product;
import bear.game.company.strategies.CompanyStrategy;
import bear.game.company.strategies.DailyOutput;

public class Environment {
    
    public List<Opportunity> contracts;
    public List<Opportunity> openMarket;

    public Random random = new Random();


    public void simulateYear(List<CompanyStrategy> strategies) {

        Map<Opportunity, List<Offer>> contractOffers = new HashMap<>();
        Map<Opportunity, Integer> offerCutOffDay = new HashMap<>();

        // similuate 365 days
        List<Integer> days = IntStream.range(0, 365).boxed().toList();
        for (Integer day : days) {
            
            // get daily output from each company
            List<Product> producedProducts = new ArrayList<>();
            for (CompanyStrategy strategy : strategies) {
                DailyOutput output = strategy.performDailyTasks(openMarket, contracts);
                output.offers.stream()
                    .forEach(offer -> {
                        if (contractOffers.containsKey(offer.opportunity)) {
                            contractOffers.get(offer.opportunity).add(offer);
                        }
                    });

                producedProducts.addAll(output.products);
            }

            // determine contract winners
            offerCutOffDay.entrySet().stream()
                .filter(entry -> entry.getValue() == day)
                .forEach(entry -> {
                    Opportunity contract = entry.getKey();
                    determineContractWinner(strategies, contract, contractOffers.get(contract));
                });

            // determine finished contract rewards
            producedProducts.stream()
                .filter(ContractProduct.class::isInstance)
                .forEach(product -> {
                    ContractProduct contractProduct = (ContractProduct) product;
                    CompanyStrategy company = contractProduct.opportunity.offer.company;
                    Integer reward = evaluateProduct(contractProduct, contractProduct.opportunity.offer);
                    company.reputation += reward;
                    company.money += contractProduct.price;
                });

            // determine open market rewards
            producedProducts.stream()
                .filter(Product.class::isInstance)
                .forEach(product -> {
                    Opportunity opportunity = product.opportunity;
                    if (opportunity.value > 0) {

                        Integer reward = product.quality + product.price + product.advertisingEffort;
                    
                        opportunity.value -= reward;
                    }
                });


            // make new contracts and open market opportunities. Remove some open market opportunities
            if (random.nextInt(100) < 10) {
                Opportunity newContract = new Opportunity();
                newContract.value = random.nextInt(1000);
                newContract.complexity = random.nextInt(100);
                contracts.add(newContract);

                contractOffers.put(newContract, new ArrayList<>());
                offerCutOffDay.put(newContract, random.nextInt(10, 30));
            }
            else if (random.nextInt(100) < 10) {
                Opportunity newOpportunity = new Opportunity();
                newOpportunity.value = random.nextInt(10000);
                newOpportunity.complexity = random.nextInt(100);
                openMarket.add(newOpportunity);

                openMarket.remove(random.nextInt(openMarket.size()));
            }
            
        }
    }

    private void determineContractWinner(List<CompanyStrategy> strategies, Opportunity contract, List<Offer> offers) {
        
        Offer bestOffer = offers.stream()
            .collect(Collectors.toMap(offer -> offer, this::evaluateOffer))
            .entrySet()
            .stream()
            .max(Map.Entry.comparingByValue())
            .map(Map.Entry::getKey)
            .orElse(null);

        if (bestOffer != null) {
            CompanyStrategy chosenCompany = bestOffer.company;
            contract.offer = bestOffer;
            chosenCompany.contracts.add(contract);
        }
    }

    private Integer evaluateOffer(Offer offer) {
        Integer value = offer.company.reputation;
        value -= offer.price;
        value -= offer.timeToComplete;
        value += offer.salesAndAdvertisingEffort;

        return value;
    }

    private Integer evaluateProduct(ContractProduct product, Offer offer) {
        Integer value = product.quality;
        value -= product.price;
        value -= Math.abs(product.timeToComplete - offer.timeToComplete);

        return value;
    }


    /**
     * Open market performance
     */
    private void competeInOpenMarket(List<CompanyStrategy> strategies) {
        

    }



}
