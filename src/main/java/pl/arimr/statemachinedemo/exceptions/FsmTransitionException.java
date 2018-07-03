package pl.arimr.statemachinedemo.exceptions;


public class FsmTransitionException extends Exception {

    public FsmTransitionException() {
        this("FSM TRANSITION EXCEPTION");
    }

    public FsmTransitionException(String message) {
        super(message);
    }
}
