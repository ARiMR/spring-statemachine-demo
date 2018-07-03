package pl.arimr.statemachinedemo.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.statemachine.persist.StateMachinePersister;
import org.springframework.stereotype.Service;
import pl.arimr.statemachinedemo.domain.Application;
import pl.arimr.statemachinedemo.enums.ApplicationEvent;
import pl.arimr.statemachinedemo.enums.ApplicationStatus;

import javax.persistence.EntityManager;
import javax.transaction.Transactional;

@Transactional
@Service
public class ApplicationFsmService extends AbstractStateMachineService<ApplicationStatus, ApplicationEvent, Application> {

    @Autowired
    public ApplicationFsmService(final ApplicationContext applicationContext,
                                 final StateMachinePersister<ApplicationStatus, ApplicationEvent, Application> persister,
                                 final EntityManager entityManager) {
        super(applicationContext, persister, entityManager);
    }

    @Override
    protected String getFsmFactoryName() {
        return "applicationFsm";
    }

    @Override
    protected ApplicationStatus getEntityState(final Application entity) {
        return entity.getStatus();
    }
}
