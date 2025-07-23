package com.qp.quantum_share.repository;

import com.qp.quantum_share.dto.Drafts;
import com.qp.quantum_share.dto.DraftsResponseDto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DraftRepository extends JpaRepository<Drafts, Integer> {

    @Query("SELECT new com.qp.quantum_share.dto.DraftsResponseDto(d.draftId, d.caption, d.title, d.visibility, d.userTimeZone, d.boardName, d.postUrl, d.contentType, d.fileName) " +
            "FROM Drafts d WHERE d.user.userId = :userId Order By d.draftId DESC")
    List<DraftsResponseDto> findByUserId(@Param("userId") int userId);

}
