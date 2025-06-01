package study.data_jpa.repository;

import jakarta.persistence.LockModeType;
import jakarta.persistence.QueryHint;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import study.data_jpa.dto.MemberDto;
import study.data_jpa.entity.Member;

import java.util.List;

public interface MemberRepository extends JpaRepository<Member, Long>, MemberRepositoryCustom {
    List<Member> findByUsernameAndAgeGreaterThan(String name, int age);
    List<Member> findTop3HelloBy();
    Member findByUsername(String username);

    @Query("select m from Member m where m.username = :username and m.age = :age")
    List<Member> findUser(@Param("username") String username, @Param("age") int age);

    @Query("select m.username from Member m")
    List<String> findUsernameList();

    @Query("select new study.data_jpa.dto.MemberDto(m.id, m.username, t.name) from Member m join m.team t")
    List<MemberDto> findMemberDto();

    Page<Member> findByAge(int age, Pageable pageable);
    //Slice<Member> findByAge(int age, Pageable pageable);

    // clearAutomatically = true 속성을 지정해주면, Bulk연산 이후 영속성 컨텍스트에 남아있는 데이터를 DB에 반영한다/날린다.
    // EntityManger.clear() 별도 호출 안해도 됨. MemberRepositoryTest.bulkUpdateAge 테스트 소스 참고
    @Modifying(clearAutomatically = true)
    @Query("update Member m set m.age = m.age + 1 where m.age >= :age")
    int bulkAgePlus(@Param("age")int age);

    @Query("select m from Member m left join fetch m.team")
    List<Member> findMemberFetchJoin();

    @Override
    @EntityGraph(attributePaths = { "team" })
    List<Member> findAll();

    @EntityGraph(attributePaths = { "team" })
    @Query("select m from Member m")
    List<Member> findMemberEntityGraph();

    @EntityGraph(attributePaths = { "team" })
    List<Member> findEntityGraphByUsername(String username);

    // 조회용 힌트
    // 조회용 로직에 무조건 넣으면 x, 힌트를 추가했을 때와 그러지 않았을 때 성능 차이가 드라마틱하지 않으므로
    // 힌트 추가 전후로 성능 비교를 해서 사용할 것
    @QueryHints(value = @QueryHint(name = "org.hibernate.readOnly", value = "true"))
    Member findReadOnlyByUsername(String username);

    @Lock(LockModeType.PESSIMISTIC_READ)
    List<Member> findLockByUsername(String username);
}
