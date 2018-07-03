package pl.arimr.statemachinedemo;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;
import pl.arimr.statemachinedemo.domain.Application;
import pl.arimr.statemachinedemo.enums.ApplicationEvent;
import pl.arimr.statemachinedemo.enums.ApplicationStatus;
import pl.arimr.statemachinedemo.exceptions.FsmTransitionException;
import pl.arimr.statemachinedemo.repositories.ApplicationRespository;
import pl.arimr.statemachinedemo.service.ApplicationFsmService;

import javax.transaction.Transactional;
import java.math.BigDecimal;

@RunWith(SpringRunner.class)
@SpringBootTest
@Transactional
@SuppressWarnings("duplicates")
public class ApplicationFsmTests {

    @Autowired
    private ApplicationRespository applicationRespository;

    @Autowired
    private ApplicationFsmService applicationFsmService;

    @Test
    public void check_initial_status() {
        Application application = new Application("A", "X", BigDecimal.ZERO);
        Assert.assertEquals(ApplicationStatus.ENTERED, application.getStatus());
        application = applicationRespository.save(application);
        Assert.assertEquals(ApplicationStatus.ENTERED, application.getStatus());
    }

    @Test
    public void accept_entered_entity() throws FsmTransitionException {
        Application application = applicationRespository.save(new Application("A", "X", BigDecimal.ZERO));
        Assert.assertEquals(ApplicationStatus.ENTERED, application.getStatus());
        applicationFsmService.sendEvent(application, ApplicationEvent.ACCEPT);

        application = applicationRespository.getOne(application.getId());
        Assert.assertEquals(ApplicationStatus.ACCEPTED, application.getStatus());
    }

    @Test(expected = FsmTransitionException.class)
    public void cannot_accept_unsaved_entity() throws FsmTransitionException {
        Application application = new Application("A", "X", BigDecimal.ZERO);
        Assert.assertEquals(ApplicationStatus.ENTERED, application.getStatus());
        applicationFsmService.sendEvent(application, ApplicationEvent.ACCEPT);
    }

    @Test(expected = FsmTransitionException.class)
    public void cannot_accept_allready_accepted_entity() throws FsmTransitionException {
        Application application = applicationRespository.save(new Application("A", "X", BigDecimal.ZERO));
        Assert.assertEquals(ApplicationStatus.ENTERED, application.getStatus());
        applicationFsmService.sendEvent(application, ApplicationEvent.ACCEPT);

        application = applicationRespository.getOne(application.getId());
        Assert.assertEquals(ApplicationStatus.ACCEPTED, application.getStatus());

        applicationFsmService.sendEvent(application, ApplicationEvent.ACCEPT);
    }

    @Test(expected = FsmTransitionException.class)
    public void cannot_approve_entered_entity() throws FsmTransitionException {
        Application application = new Application("A", "X", BigDecimal.ZERO);
        Assert.assertEquals(ApplicationStatus.ENTERED, application.getStatus());
        applicationFsmService.sendEvent(application, ApplicationEvent.APPROVE);
    }

    @Test(expected = FsmTransitionException.class)
    public void cannot_discard_entered_entity() throws FsmTransitionException {
        Application application = new Application("A", "X", BigDecimal.ZERO);
        Assert.assertEquals(ApplicationStatus.ENTERED, application.getStatus());
        applicationFsmService.sendEvent(application, ApplicationEvent.DISCARD);
    }

    @Test
    public void accept_discard_path() throws FsmTransitionException {
        Application application = applicationRespository.save(new Application("A", "X", BigDecimal.ZERO));
        Assert.assertEquals(ApplicationStatus.ENTERED, application.getStatus());
        applicationFsmService.sendEvent(application, ApplicationEvent.ACCEPT);

        application = applicationRespository.getOne(application.getId());
        Assert.assertEquals(ApplicationStatus.ACCEPTED, application.getStatus());

        applicationFsmService.sendEvent(application, ApplicationEvent.DISCARD);
        Assert.assertEquals(ApplicationStatus.ENTERED, application.getStatus());
    }

    @Test(expected = FsmTransitionException.class)
    public void cannot_approve_with_null_amount() throws FsmTransitionException {
        Application application = applicationRespository.save(new Application("A", "X", null));
        Assert.assertEquals(ApplicationStatus.ENTERED, application.getStatus());
        applicationFsmService.sendEvent(application, ApplicationEvent.ACCEPT);

        application = applicationRespository.getOne(application.getId());
        Assert.assertEquals(ApplicationStatus.ACCEPTED, application.getStatus());

        applicationFsmService.sendEvent(application, ApplicationEvent.APPROVE);
    }

    @Test(expected = FsmTransitionException.class)
    public void cannot_approve_with_zero_amount() throws FsmTransitionException {
        Application application = applicationRespository.save(new Application("A", "X", BigDecimal.ZERO));
        Assert.assertEquals(ApplicationStatus.ENTERED, application.getStatus());
        applicationFsmService.sendEvent(application, ApplicationEvent.ACCEPT);

        application = applicationRespository.getOne(application.getId());
        Assert.assertEquals(ApplicationStatus.ACCEPTED, application.getStatus());

        applicationFsmService.sendEvent(application, ApplicationEvent.APPROVE);
    }

    @Test(expected = FsmTransitionException.class)
    public void cannot_approve_with_amount_less_than_zero() throws FsmTransitionException {
        Application application = applicationRespository.save(new Application("A", "X", BigDecimal.valueOf(-42L)));
        Assert.assertEquals(ApplicationStatus.ENTERED, application.getStatus());
        applicationFsmService.sendEvent(application, ApplicationEvent.ACCEPT);

        application = applicationRespository.getOne(application.getId());
        Assert.assertEquals(ApplicationStatus.ACCEPTED, application.getStatus());

        applicationFsmService.sendEvent(application, ApplicationEvent.APPROVE);
    }

    @Test
    public void happy_path() throws FsmTransitionException {
        Application application = applicationRespository.save(new Application("A", "X", BigDecimal.TEN));
        Assert.assertEquals(ApplicationStatus.ENTERED, application.getStatus());
        applicationFsmService.sendEvent(application, ApplicationEvent.ACCEPT);

        application = applicationRespository.getOne(application.getId());
        Assert.assertEquals(ApplicationStatus.ACCEPTED, application.getStatus());

        applicationFsmService.sendEvent(application, ApplicationEvent.APPROVE);
        Assert.assertEquals(ApplicationStatus.APPROVED, application.getStatus());
        Assert.assertEquals("APPROVED: A", application.getName());

        application = applicationRespository.getOne(application.getId());
        Assert.assertEquals(ApplicationStatus.APPROVED, application.getStatus());
        Assert.assertEquals("APPROVED: A", application.getName());
    }
}
