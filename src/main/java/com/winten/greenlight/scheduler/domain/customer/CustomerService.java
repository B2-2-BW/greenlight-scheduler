
package com.winten.greenlight.scheduler.domain.customer;

import com.winten.greenlight.scheduler.db.repository.redis.customer.CustomerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class CustomerService {

    private final CustomerRepository customerRepository;

    public void relocateCustomerBy(Long actionGroupId, int availableCapacity) {
        if (availableCapacity < 1) {
            log.info("No customers to relocate. Skipping.");
            return;
        }

        List<Customer> customers = customerRepository.getTopNCustomersBy(actionGroupId, availableCapacity);

        for (Customer customer : customers) {
            try {
                customerRepository.createCustomerBy(customer);
                customer.setWaitStatus(WaitStatus.WAITING);
                customerRepository.deleteCustomerBy(customer);
                log.info("Successfully relocated: {}", customer);
            } catch (Exception e) {
                log.error("Error relocating customer: {}", customer, e);
            }
        }
    }
}