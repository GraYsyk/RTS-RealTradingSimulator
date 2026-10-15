package net.graysenko.RTS.Persistent.Repositories;

import net.graysenko.RTS.MatchingEngine.Enums.Status;
import net.graysenko.RTS.Persistent.Entities.OrderEntity;
import net.graysenko.RTS.Persistent.Entities.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OrderRepository extends JpaRepository<OrderEntity, Long> {
    List<OrderEntity> findByUserAndStatus(User user, Status status);
    List<OrderEntity> findByStatusIn(List<Status> statuses);
}
