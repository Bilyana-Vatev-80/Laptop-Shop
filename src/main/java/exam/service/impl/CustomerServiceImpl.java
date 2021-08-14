package exam.service.impl;

import com.google.gson.Gson;
import exam.model.dto.CustomerSeedDto;
import exam.model.entity.Customer;
import exam.repository.CustomerRepository;
import exam.service.CustomerService;
import exam.service.TownService;
import exam.util.ValidationUtil;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;

@Service
public class CustomerServiceImpl implements CustomerService {
    private static final String CUSTOMER_FILE_PATH = "src/main/resources/files/json/customers.json";
    private final CustomerRepository customerRepository;
    private final TownService townService;
    private final Gson gson;
    private final ValidationUtil validationUtil;
    private final ModelMapper modelMapper;

    public CustomerServiceImpl(CustomerRepository customerRepository, TownService townService, Gson gson, ValidationUtil validationUtil, ModelMapper modelMapper) {
        this.customerRepository = customerRepository;
        this.townService = townService;
        this.gson = gson;
        this.validationUtil = validationUtil;
        this.modelMapper = modelMapper;
    }

    @Override
    public boolean areImported() {
        return this.customerRepository.count() > 0;
    }

    @Override
    public String readCustomersFileContent() throws IOException {
        return Files.readString(Path.of(CUSTOMER_FILE_PATH));
    }

    @Override
    public String importCustomers() throws IOException {
        StringBuilder sb = new StringBuilder();

        CustomerSeedDto[] customerSeedDtos = gson.fromJson(readCustomersFileContent(), CustomerSeedDto[].class);

        Arrays.stream(customerSeedDtos).filter(customerSeedDto -> {
            boolean isValid = validationUtil.isValid(customerSeedDto)
                    && !existsCustomerByEmail(customerSeedDto.getEmail())
                    && townService.existsTownByName(customerSeedDto.getTown().getName());
            sb
                    .append(isValid ? String.format("Successfully imported Customer %s %s - %s", customerSeedDto.getFirstName(),customerSeedDto.getLastName(),customerSeedDto.getEmail())
                            : "Invalid Customer")
                    .append(System.lineSeparator());

            return isValid;
        })
                .map(customerSeedDto -> {
                    Customer customer = modelMapper.map(customerSeedDto, Customer.class);
                    customer.setTown(townService.findTownByName(customerSeedDto.getTown().getName()));

                    return customer;
                })
                .forEach(this.customerRepository::save);
        return sb.toString();
    }

    @Override
    public boolean existsCustomerByEmail(String email) {
        return this.customerRepository.existsCustomerByEmail(email);
    }
}
