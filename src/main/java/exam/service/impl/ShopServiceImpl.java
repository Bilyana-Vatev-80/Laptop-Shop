package exam.service.impl;

import exam.model.dto.ShopSeedRootDto;
import exam.model.entity.Shop;
import exam.repository.ShopRepository;
import exam.service.ShopService;
import exam.service.TownService;
import exam.util.ValidationUtil;
import exam.util.XmlParser;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;

import javax.xml.bind.JAXBException;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@Service
public class ShopServiceImpl implements ShopService {
    private static final String SHOP_FILE_PATH = "src/main/resources/files/xml/shops.xml";
    private final ShopRepository shopRepository;
    private final TownService townService;
    private final XmlParser xmlParser;
    private final ValidationUtil validationUtil;
    private final ModelMapper modelMapper;

    public ShopServiceImpl(ShopRepository shopRepository, TownService townService, XmlParser xmlParser, ValidationUtil validationUtil, ModelMapper modelMapper) {
        this.shopRepository = shopRepository;
        this.townService = townService;
        this.xmlParser = xmlParser;
        this.validationUtil = validationUtil;
        this.modelMapper = modelMapper;
    }

    @Override
    public boolean areImported() {
        return this.shopRepository.count() > 0;
    }

    @Override
    public String readShopsFileContent() throws IOException {
        return Files.readString(Path.of(SHOP_FILE_PATH));
    }

    @Override
    public String importShops() throws JAXBException, FileNotFoundException {
        StringBuilder sb = new StringBuilder();

        ShopSeedRootDto shopSeedRootDtos = xmlParser.fromFile(SHOP_FILE_PATH, ShopSeedRootDto.class);

        shopSeedRootDtos.getShops()
                .stream()
                .filter(shopSeedDto -> {
                    boolean isValid = validationUtil.isValid(shopSeedDto)
                            && !existsShopByName(shopSeedDto.getName())
                            && townService.existsTownByName(shopSeedDto.getTown().getName());
                    sb
                            .append(isValid ? String.format("Successfully imported Shop %s - %.0f", shopSeedDto.getName(),shopSeedDto.getIncome())
                                    : "Invalid shop")
                            .append(System.lineSeparator());

                    return isValid;
                })
                .map(shopSeedDto -> {
                    Shop shop = modelMapper.map(shopSeedDto, Shop.class);
                    shop.setTown(townService.findTownByName(shopSeedDto.getTown().getName()));

                    return shop;
                })
                .forEach(this.shopRepository::save);
        return sb.toString();
    }

    @Override
    public boolean existsShopByName(String name) {
        return this.shopRepository.existsShopByName(name);
    }

    @Override
    public Shop findShopByName(String name) {
        return this.shopRepository.findShopByName(name);
    }
}
