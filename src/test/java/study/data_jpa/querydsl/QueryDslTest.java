package study.data_jpa.querydsl;

import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.Rollback;
import org.springframework.transaction.annotation.Transactional;
import study.data_jpa.entity.Member;
import study.data_jpa.entity.QMember;
import study.data_jpa.entity.Team;

import static org.assertj.core.api.Assertions.assertThat;
import static study.data_jpa.entity.QMember.member;

@SpringBootTest
@Transactional
@Rollback(false)
public class QueryDslTest {

    @Autowired
    EntityManager em;

    JPAQueryFactory queryFactory;

    /*@Test
    public void contextLoads() {
        Member member = new Member("memberA");
        em.persist(member);

        JPAQueryFactory query = new JPAQueryFactory(em);

        QMember qMember = new QMember("m");

        Member result = query.selectFrom(qMember).fetchOne();

        assertThat(member.getId()).isEqualTo(result.getId());
    }*/

    @BeforeEach
    public void before() {
        queryFactory = new JPAQueryFactory(em);

        Team teamA = new Team("teamA");
        Team teamB = new Team("teamB");

        em.persist(teamA);
        em.persist(teamB);

        Member member1 = new Member("member1", 10, teamA);
        Member member2 = new Member("member2", 20, teamA);
        Member member3 = new Member("member3", 30, teamB);
        Member member4 = new Member("member4", 40, teamB);

        em.persist(member1);
        em.persist(member2);
        em.persist(member3);
        em.persist(member4);
    }

    @Test
    void start() {
        //QMember m = new QMember("m"); // variable (별칭같은 것) 지정
        //QMember m = QMember.member; // 별칭을 지정하지 않아도 기본적으로 설정해주는 값으로 선언 가능

        Member findMember = queryFactory
                .select(member) //static 타입으로 선언해서 활용 가능
                .from(member)
                .where(member.username.eq("member1"))
                .fetchOne();

        assertThat(findMember.getUsername()).isEqualTo("member1");
    }

    @Test
    void search() {
        Member findMember = queryFactory
                .selectFrom(member) // select(member).from(member) 를 selectFrom 으로 축약해서 사용 가능
                .where(
                        //member.username.eq("member1")
                        //.and(member.age.eq(10))

                        // ,로 각 조건을 줄 수도 있음(and 조건과 같음)
                        member.username.eq("member1"),
                        member.age.eq(10)
                )
                .fetchOne();

        assertThat(findMember.getUsername()).isEqualTo("member1");
    }
}
