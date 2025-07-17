package com.winten.greenlight.scheduler.db.repository.redis.customer;

import com.winten.greenlight.scheduler.domain.customer.Customer;
import com.winten.greenlight.scheduler.domain.customer.WaitStatus;
import com.winten.greenlight.scheduler.support.error.CoreException;
import com.winten.greenlight.scheduler.support.error.ErrorType;
import com.winten.greenlight.scheduler.support.util.RedisKeyBuilder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Slf4j
@Repository
public class CustomerRepository {

    private final StringRedisTemplate redisTemplate;
    private final ZSetOperations<String, String> zSetOperations;
    private final RedisKeyBuilder redisKeyBuilder;

    public CustomerRepository(StringRedisTemplate redisTemplate, RedisKeyBuilder redisKeyBuilder) {
        this.redisTemplate = redisTemplate;
        this.zSetOperations = redisTemplate.opsForZSet();
        this.redisKeyBuilder = redisKeyBuilder;
    }

    public void createCustomerBy(Customer customer) {
        //Boolean result = zSetOperations.add(customer.getWaitStatus()..queueName(), customer.getCustomerId(), customer.getScore());
        String redisKey = redisKeyBuilder.actionGroupQueue(customer.getActionId(),customer.getWaitStatus());
        Boolean result = zSetOperations.add(redisKey, customer.getCustomerId(), customer.getScore());

        if (Boolean.TRUE.equals(result)) {
        } else {
            throw CoreException.of(ErrorType.REDIS_ERROR, "Customer Not Saved");
        }
    }

    public void deleteCustomerBy(Customer customer) {
        //Long removedCount = zSetOperations.remove(customer.getWaitingPhase().queueName(), customer.getCustomerId());
        String redisKey = redisKeyBuilder.actionGroupQueue(customer.getActionId(),customer.getWaitStatus());
        Long removedCount = zSetOperations.remove(redisKey, customer.getCustomerId());

        if (removedCount != null && removedCount > 0) {
        } else {
        }
    }

    /**
     * ActionGroup ID별
     * @param actionGroupId
     * @param availableCapacity
     * @return
     */
    public List<Customer> getTopNCustomersBy(Long actionGroupId, Long availableCapacity) {
        //Set<ZSetOperations.TypedTuple<String>> tuples = zSetOperations.rangeWithScores(WaitingPhase.WAITING.queueName(), 0, eventBackPressure - 1);
        String redisKey = redisKeyBuilder.actionGroupQueue(actionGroupId, WaitStatus.WAITING);
        Set<ZSetOperations.TypedTuple<String>> tuples = zSetOperations.rangeWithScores(redisKey, 0, availableCapacity -1);

        List<Customer> customers = new ArrayList<>();
        if (tuples != null) {
            for (ZSetOperations.TypedTuple<String> tuple : tuples) {
                customers.add(new Customer(actionGroupId, tuple.getValue(), tuple.getScore(), WaitStatus.READY));
            }
        }

        return customers;
    }


}
