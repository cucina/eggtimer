package org.cucina.eggtimer.service;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.StringUtils;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Required;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHandler;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.util.Assert;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * JAVADOC for Class Level
 *
 * @author $Author: $
 * @version $Revision: $
  */
public class SchedulingServiceImpl
    implements SchedulingService, MessageHandler {
    private static final Logger LOG = LoggerFactory.getLogger(SchedulingServiceImpl.class);
    private Map<String, ScheduledFuture<?>> runningSchedules = new HashMap<String, ScheduledFuture<?>>();
    private MessageChannel channel;
    private ScheduleRepository scheduleRepository;
    private ScheduledExecutorService executorService = Executors.newScheduledThreadPool(1);

    /**
    * Creates a new SchedulingServiceImpl object.
    *
    * @param channel JAVADOC.
    */
    public SchedulingServiceImpl(MessageChannel channel) {
        Assert.notNull(channel, "channel is null");
        this.channel = channel;
    }

    /**
     * JAVADOC Method Level Comments
     *
     * @param executorService JAVADOC.
     */
    public void setExecutorService(ScheduledExecutorService executorService) {
        this.executorService = executorService;
    }

    /**
     * JAVADOC Method Level Comments
     *
     * @param scheduleRepository JAVADOC.
     */
    @Autowired
    @Required
    public void setScheduleRepository(ScheduleRepository scheduleRepository) {
        this.scheduleRepository = scheduleRepository;
    }

    /**
     * JAVADOC Method Level Comments
     *
     * @param name JAVADOC.
     */
    @Override
    public void cancel(String name) {
        if (StringUtils.isEmpty(name)) {
            LOG.debug("Empty name for cancel");

            return;
        }

        scheduleRepository.removeByName(name);

        ScheduledFuture<?> sf = runningSchedules.get(name);

        if (sf != null) {
            sf.cancel(true);
            runningSchedules.remove(name);
            LOG.debug("Cancelled schedule '" + name + "'");
        } else {
            LOG.debug("No schedule for '" + name + "' found");
        }
    }

    /**
     * JAVADOC Method Level Comments
     *
     * @param message JAVADOC.
     *
     * @throws MessagingException JAVADOC.
     */
    @Override
    public void handleMessage(Message<?> message)
        throws MessagingException {
        Object payload = message.getPayload();

        if (payload instanceof ScheduleRequest) {
            ScheduleRequest sr = (ScheduleRequest) payload;

            if (StringUtils.isEmpty(sr.getDestination()) || (sr.getDelay() <= 0)) {
                cancel(sr.getName());
            } else {
                schedule(sr);
            }
        } else {
            LOG.warn("Payload is not a ScheduleRequest:" + payload);
        }
    }

    /**
     * JAVADOC Method Level Comments
     *
     * @param scheduleRequest JAVADOC.
     */
    @Override
    public void schedule(ScheduleRequest scheduleRequest) {
        if (LOG.isDebugEnabled()) {
            LOG.debug("Received request:" + scheduleRequest);
        }

        String destination = scheduleRequest.getDestination();
        String message = scheduleRequest.getMessage();

        if (StringUtils.isEmpty(destination) || StringUtils.isEmpty(message)) {
            LOG.warn("Either destination or message are empty, no scheduling");
        }

        long delay = scheduleRequest.getDelay();
        TimeUnit unit = scheduleRequest.getTimeUnit();

        if (unit == null) {
            unit = TimeUnit.SECONDS;
        }

        Long period = scheduleRequest.getPeriod();
        ScheduledFuture<?> sf;

        if (period == null) {
            sf = executorService.schedule(createCommand(destination, message), delay, unit);
        } else {
            sf = executorService.scheduleAtFixedRate(createCommand(destination, message), delay,
                    period, unit);
        }

        String name = scheduleRequest.getName();

        if (LOG.isDebugEnabled()) {
            LOG.debug("Saving schedule for name '" + name + "'");
        }

        scheduleRepository.save(scheduleRequest);
        runningSchedules.put(name, sf);
    }

    private Runnable createCommand(final String destination, final String messageText) {
        return new Runnable() {
                @Override
                public void run() {
                    if (LOG.isDebugEnabled()) {
                        LOG.debug("Invoked for destination '" + destination + "' and message '" +
                            messageText + "'");
                    }

                    Message<String> message = MessageBuilder.withPayload(messageText)
                                                            .setHeader("destination", destination)
                                                            .build();

                    channel.send(message);
                }
            };
    }
}
