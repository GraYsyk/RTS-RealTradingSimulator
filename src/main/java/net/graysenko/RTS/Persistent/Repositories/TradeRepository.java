package net.graysenko.RTS.Persistent.Repositories;

import net.graysenko.RTS.Persistent.Entities.TradeEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TradeRepository extends JpaRepository<TradeEntity, Long> {

}
