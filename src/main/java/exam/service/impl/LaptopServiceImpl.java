package exam.service.impl;

import com.google.gson.Gson;
import exam.model.dto.LaptopSeedDto;
import exam.model.entity.Laptop;
import exam.repository.LaptopRepository;
import exam.service.LaptopService;
import exam.service.ShopService;
import exam.util.ValidationUtil;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

@Service
public class LaptopServiceImpl implements LaptopService {
    private static final String LAPTOP_FILE_PATH = "src/main/resources/files/json/laptops.json";
    private final LaptopRepository laptopRepository;
    private final ShopService shopService;
    private final Gson gson;
    private final ValidationUtil validationUtil;
    private final ModelMapper modelMapper;

    public LaptopServiceImpl(LaptopRepository laptopRepository, ShopService shopService, Gson gson, ValidationUtil validationUtil, ModelMapper modelMapper) {
        this.laptopRepository = laptopRepository;
        this.shopService = shopService;
        this.gson = gson;
        this.validationUtil = validationUtil;
        this.modelMapper = modelMapper;
    }

    @Override
    public boolean areImported() {
        return this.laptopRepository.count() > 0;
    }

    @Override
    public String readLaptopsFileContent() throws IOException {
        return Files.readString(Path.of(LAPTOP_FILE_PATH));
    }

    @Override
    public String importLaptops() throws IOException {
        StringBuilder sb = new StringBuilder();

        LaptopSeedDto[] laptopSeedDtos = gson.fromJson(readLaptopsFileContent(), LaptopSeedDto[].class);

        Arrays.stream(laptopSeedDtos).filter(laptopSeedDto -> {
            boolean isValid = validationUtil.isValid(laptopSeedDto)
                    && !existsLaptopByMacAddress(laptopSeedDto.getMacAddress())
                    && shopService.existsShopByName(laptopSeedDto.getShop().getName());
            sb
                    .append(isValid ? String.format("Successfully imported Laptop %s - %.2f - %d - %d", laptopSeedDto.getMacAddress(),laptopSeedDto.getCpuSpeed(),
                            laptopSeedDto.getRam(),laptopSeedDto.getStorage())
                            : "Invalid Laptop")
                    .append(System.lineSeparator());

            return isValid;
        })
                .map(laptopSeedDto -> {
                    Laptop laptop = modelMapper.map(laptopSeedDto, Laptop.class);
                    laptop.setShop(shopService.findShopByName(laptopSeedDto.getShop().getName()));

                    return laptop;
                })
                .forEach(this.laptopRepository::save);
        return sb.toString();
    }

    @Override
    public String exportBestLaptops() {
        StringBuilder sb = new StringBuilder();

        List<Laptop> laptops = this.laptopRepository.findAllBestLaptop();
        laptops.forEach(laptop -> {
            sb
                    .append(String.format("Laptop - %s\n" +
                            "*Cpu speed - %.2f\n" +
                            "**Ram - %d\n" +
                            "***Storage - %d\n" +
                            "****Price - %.2f\n" +
                            "#Shop name - %s\n" +
                            "##Town - %s\n", laptop.getMacAddress(),
                            laptop.getCpuSpeed(),
                            laptop.getRam(),
                            laptop.getStorage(),
                            laptop.getPrice(),
                            laptop.getShop().getName(),
                            laptop.getShop().getTown().getName()))
                    .append(System.lineSeparator());
        });
        return sb.toString();
    }

    @Override
    public boolean existsLaptopByMacAddress(String macAddress) {
        return this.laptopRepository.existsLaptopByMacAddress(macAddress);
    }
}
