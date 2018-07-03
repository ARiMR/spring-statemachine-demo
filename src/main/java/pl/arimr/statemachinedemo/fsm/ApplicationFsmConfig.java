package pl.arimr.statemachinedemo.fsm;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;
import org.springframework.messaging.Message;
import org.springframework.statemachine.StateMachine;
import org.springframework.statemachine.StateMachineContext;
import org.springframework.statemachine.StateMachinePersist;
import org.springframework.statemachine.action.Action;
import org.springframework.statemachine.config.StateMachineBuilder;
import org.springframework.statemachine.guard.Guard;
import org.springframework.statemachine.listener.StateMachineListener;
import org.springframework.statemachine.listener.StateMachineListenerAdapter;
import org.springframework.statemachine.persist.DefaultStateMachinePersister;
import org.springframework.statemachine.persist.StateMachinePersister;
import org.springframework.statemachine.state.State;
import org.springframework.statemachine.support.DefaultExtendedState;
import org.springframework.statemachine.support.DefaultStateMachineContext;
import pl.arimr.statemachinedemo.cons.FsmExtendedStateCons;
import pl.arimr.statemachinedemo.domain.Application;
import pl.arimr.statemachinedemo.enums.ApplicationEvent;
import pl.arimr.statemachinedemo.enums.ApplicationStatus;

import javax.persistence.EntityManager;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Configuration
public class ApplicationFsmConfig {

    @Bean
    public StateMachinePersister<ApplicationStatus, ApplicationEvent, Application> applicationFsmPersister(
            final StateMachinePersist<ApplicationStatus, ApplicationEvent, Application> persist) {
        return new DefaultStateMachinePersister<>(persist);
    }

    @Bean
    public StateMachinePersist<ApplicationStatus, ApplicationEvent, Application> applicationFsmPersist(final EntityManager em) {
        return new StateMachinePersist<ApplicationStatus, ApplicationEvent, Application>() {

            @Override
            public void write(final StateMachineContext<ApplicationStatus, ApplicationEvent> context, Application entity) throws NoSuchFieldException, IllegalAccessException {
                ApplicationStatus status = context.getState();
                em.createNativeQuery("UPDATE APPLICATION SET STATUS=:status WHERE ID=:id")
                        .setParameter("id", entity.getId())
                        .setParameter("status", status.name())
                        .executeUpdate();
                // set status via reflection to avoid having setter
                Field field = entity.getClass().getDeclaredField("status");
                field.setAccessible(true);
                field.set(entity, status);
            }

            @Override
            public StateMachineContext<ApplicationStatus, ApplicationEvent> read(final Application entity) {
                Map<Object, Object> extendedState = new HashMap<>();
                extendedState.put(FsmExtendedStateCons.ENTITY, entity);
                return new DefaultStateMachineContext<>(entity.getStatus(), null, new HashMap<>(), new DefaultExtendedState(extendedState));
            }
        };
    }

    @Bean
    public StateMachineListener<ApplicationStatus, ApplicationEvent> applicationFsmloggingListener() {
        return new StateMachineListenerAdapter<ApplicationStatus, ApplicationEvent>() {
            @Override
            public void stateChanged(State<ApplicationStatus, ApplicationEvent> from, State<ApplicationStatus, ApplicationEvent> to) {
                log.info("FSM State changed to {}", to.getId());
            }

            @Override
            public void eventNotAccepted(Message<ApplicationEvent> event) {
                log.error("FSM Event not accepted: {}", event.getPayload());
            }

            @Override
            public void extendedStateChanged(Object key, Object value) {
                log.info("FSM state changed {}={}", key, value);
            }
        };
    }

    @Bean("applicationFsm")
    @Scope("prototype")
    public StateMachine<ApplicationStatus, ApplicationEvent> applicationFsm() throws Exception {
        StateMachineBuilder.Builder<ApplicationStatus, ApplicationEvent> builder = StateMachineBuilder.builder();

        builder.configureConfiguration()
                .withConfiguration()
                .autoStartup(true)
                .listener(applicationFsmloggingListener());

        builder.configureStates()
                .withStates()
                .initial(ApplicationStatus.ENTERED)
                .states(EnumSet.allOf(ApplicationStatus.class))
                .end(ApplicationStatus.APPROVED);

        builder.configureTransitions()
                .withExternal().event(ApplicationEvent.ACCEPT)
                .source(ApplicationStatus.ENTERED).target(ApplicationStatus.ACCEPTED)
                .guard(createAcceptGuard())
                .action(crateAcceptAction())
                .and()
                .withExternal().event(ApplicationEvent.APPROVE)
                .source(ApplicationStatus.ACCEPTED).target(ApplicationStatus.APPROVED)
                .guard(createApproveGuard())
                .action(createApproveAction())
                .and()
                .withExternal().event(ApplicationEvent.DISCARD)
                .source(ApplicationStatus.ACCEPTED).target(ApplicationStatus.ENTERED)
                .action(createDiscardAction());


        return builder.build();
    }

    private Guard<ApplicationStatus, ApplicationEvent> createAcceptGuard() {
        return stateContext -> {
            Application entity = stateContext.getExtendedState().get(FsmExtendedStateCons.ENTITY, Application.class);
            if (entity.getId() == null) {
                stateContext.getExtendedState().getVariables().put(FsmExtendedStateCons.ERROR, "Cannot accept unsaved entity");
                return false;
            }
            return true;
        };
    }


    private Action<ApplicationStatus, ApplicationEvent> crateAcceptAction() {
        return stateContext -> {
            Application entity = stateContext.getExtendedState().get(FsmExtendedStateCons.ENTITY, Application.class);
            log.info("ACCEPTED APPLICATION #" + entity.getId());
        };
    }

    private Action<ApplicationStatus, ApplicationEvent> createDiscardAction() {
        return stateContext -> {
            Application entity = stateContext.getExtendedState().get(FsmExtendedStateCons.ENTITY, Application.class);
            log.info("DISCARDED APPLICATION #" + entity.getId());
        };
    }

    private Guard<ApplicationStatus, ApplicationEvent> createApproveGuard() {
        return stateContext -> {
            Application entity = stateContext.getExtendedState().get(FsmExtendedStateCons.ENTITY, Application.class);
            if (entity.getAmount() == null) {
                stateContext.getExtendedState().getVariables().put(FsmExtendedStateCons.ERROR, "Cannot approve apprication without amount");
                return false;
            }

            if (entity.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
                stateContext.getExtendedState().getVariables().put(FsmExtendedStateCons.ERROR, "Cannot approve with incorrect amount");
                return false;
            }
            return true;
        };
    }

    private Action<ApplicationStatus, ApplicationEvent> createApproveAction() {
        return stateContext -> {
            Application entity = stateContext.getExtendedState().get(FsmExtendedStateCons.ENTITY, Application.class);
            log.info("APPROVED APPLICATION #" + entity.getId());
            entity.setName("APPROVED: " + entity.getName());
        };
    }
}

