package bear.game.company.environment.decision;

public class Decision<T> {

    public DecisionType type;
    public T consideration;


    
    public Decision(DecisionType type, T consideration) {
        this.type = type;
        this.consideration = consideration;
    }


    


}
