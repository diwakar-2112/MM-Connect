package com.MM_CONNECT.api.matching;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface UserInteractionRepository extends JpaRepository<UserInteraction, UserInteraction.InteractionKey> {

    List<UserInteraction> findByActorIdAndAction(UUID actorId, String action);

    boolean existsByActorIdAndTargetIdAndAction(UUID actorId, UUID targetId, String action);
}
