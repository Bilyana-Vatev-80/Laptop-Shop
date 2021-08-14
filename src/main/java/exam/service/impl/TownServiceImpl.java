package exam.service.impl;

import exam.model.dto.TownSeedRootDto;
import exam.model.entity.Town;
import exam.repository.TownRepository;
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
public class TownServiceImpl implements TownService {
    private static final String TOWN_FILE_PATH = "src/main/resources/files/xml/towns.xml";
    private final TownRepository townRepository;
    private final XmlParser xmlParser;
    private final ValidationUtil validationUtil;
    private final ModelMapper modelMapper;

    public TownServiceImpl(TownRepository townRepository, XmlParser xmlParser, ValidationUtil validationUtil, ModelMapper modelMapper) {
        this.townRepository = townRepository;
        this.xmlParser = xmlParser;
        this.validationUtil = validationUtil;
        this.modelMapper = modelMapper;
    }

    @Override
    public boolean areImported() {
        return this.townRepository.count() > 0;
    }

    @Override
    public String readTownsFileContent() throws IOException {
        return Files.readString(Path.of(TOWN_FILE_PATH));
    }

    @Override
    public String importTowns() throws JAXBException, FileNotFoundException {
        StringBuilder sb = new StringBuilder();

        TownSeedRootDto townSeedRootDtos = xmlParser.fromFile(TOWN_FILE_PATH, TownSeedRootDto.class);

        townSeedRootDtos.getTowns()
                .stream()
                .filter((townSeedDto -> {
                    boolean isValid = validationUtil.isValid(townSeedDto)
                            && !existsTownByName(townSeedDto.getName());
                    sb
                            .append(isValid ? String.format("Successfully imported Town %s", townSeedDto.getName())
                                    : "Invalid town")
                            .append(System.lineSeparator());

                    return isValid;
                }))
                .map(townSeedDto -> modelMapper.map(townSeedDto, Town.class))
                .forEach(this.townRepository::save);
        return sb.toString();
    }

    @Override
    public boolean existsTownByName(String name) {
        return this.townRepository.existsTownByName(name);
    }

    @Override
    public Town findTownByName(String name) {
        return this.townRepository.findTownByName(name);
    }

}
