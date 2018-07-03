package pl.arimr.statemachinedemo.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.statemachine.StateMachine;
import org.springframework.statemachine.persist.StateMachinePersister;
import pl.arimr.statemachinedemo.cons.FsmExtendedStateCons;
import pl.arimr.statemachinedemo.enums.FsmEvent;
import pl.arimr.statemachinedemo.exceptions.FsmTransitionException;

import javax.persistence.EntityManager;


public abstract class AbstractStateMachineService<S, E extends FsmEvent, T> {

    protected static final Logger log = LoggerFactory.getLogger(AbstractStateMachineService.class);

    private final StateMachinePersister<S, E, T> persister;

    private final EntityManager entityManager;

    private final ApplicationContext applicationContext;

    public AbstractStateMachineService(ApplicationContext applicationContext, StateMachinePersister<S, E, T> persister, EntityManager entityManager) {
        this.applicationContext = applicationContext;
        this.persister = persister;
        this.entityManager = entityManager;
    }

    protected abstract String getFsmFactoryName();

    protected abstract S getEntityState(T entity);

    private StateMachine<S, E> restore(T entity) {
        StateMachine<S, E> stateMachine = create(entity);
        try {
            return persister.restore(stateMachine, entity);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            throw new RuntimeException(e.getMessage(), e);
        }
    }


    private void persist(StateMachine<S, E> stateMachine, T entity) {
        try {
            persister.persist(stateMachine, entity);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    @SuppressWarnings("unchecked")
    private StateMachine<S, E> create(T entity) {
        StateMachine<S, E> stateMachine = applicationContext.getBean(getFsmFactoryName(), StateMachine.class);
        stateMachine.start();
        stateMachine.getExtendedState().getVariables().put(FsmExtendedStateCons.ENTITY, entity);
        return stateMachine;
    }


    private StateMachine<S, E> getFsm(T entity) {
        if (getEntityState(entity) != null) {
            return restore(entity);
        } else {
            return create(entity);
        }
    }

    public T sendEvent(T entity, E event) throws FsmTransitionException {
        StateMachine<S, E> fsm = getFsm(entity);
        Boolean success = fsm.sendEvent(event);
        if (success) {
            if (fsm.getExtendedState().getVariables().containsKey(FsmExtendedStateCons.ERROR)) {
                throw new FsmTransitionException(fsm.getExtendedState().getVariables().get(FsmExtendedStateCons.ERROR).toString());
            } else {
                persist(fsm, entity);
            }
        } else {
            if (fsm.getExtendedState().getVariables().containsKey(FsmExtendedStateCons.ERROR)) {
                throw new FsmTransitionException(fsm.getExtendedState().getVariables().get(FsmExtendedStateCons.ERROR).toString());
            } else {
                throw new FsmTransitionException();
            }
        }
        return entityManager.merge(entity);
    }

}
