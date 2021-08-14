package exam.repository;


import exam.model.entity.Shop;
import exam.model.entity.Town;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TownRepository extends JpaRepository<Town,Long> {

    boolean existsTownByName(String name);

    Town findTownByName(String name);
}
