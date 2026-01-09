package backend.tdms.com.service;

import backend.tdms.com.model.Customer;
import backend.tdms.com.repository.CustomerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CustomerService {

    private final CustomerRepository customerRepository;

    @Transactional
    public Customer createCustomer(Customer customer) {
        if (customerRepository.existsByPhoneNumber(customer.getPhoneNumber())) {
            throw new RuntimeException("Customer with this phone number already exists");
        }
        return customerRepository.save(customer);
    }

    public Customer getCustomerByPhone(String phoneNumber) {
        return customerRepository.findByPhoneNumber(phoneNumber)
            .orElseThrow(() -> new RuntimeException("Customer not found"));
    }

    public List<Customer> getAllCustomers() {
        return customerRepository.findAll();
    }

    @Transactional
    public Customer updateCustomer(Long id, Customer customerDetails) {
        Customer customer = customerRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Customer not found"));

        customer.setNames(customerDetails.getNames());
        customer.setPhoneNumber(customerDetails.getPhoneNumber());

        return customerRepository.save(customer);
    }
}