package study.data_jpa.querydsl;

import com.querydsl.core.QueryResults;
import com.querydsl.core.Tuple;
import com.querydsl.core.types.ExpressionUtils;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.CaseBuilder;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.PersistenceUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.Rollback;
import org.springframework.transaction.annotation.Transactional;
import study.data_jpa.dto.SimpleMemberDto;
import study.data_jpa.dto.UserDto;
import study.data_jpa.entity.Member;
import study.data_jpa.entity.QMember;
import study.data_jpa.entity.QTeam;
import study.data_jpa.entity.Team;

import java.io.IOException;
import java.util.List;

import static com.querydsl.jpa.JPAExpressions.*;
import static org.assertj.core.api.Assertions.assertThat;
import static study.data_jpa.entity.QMember.member;
import static study.data_jpa.entity.QTeam.*;

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

    @Test
    void fetch() {
        /*QueryResults<Member> results = queryFactory
                .selectFrom(member)
                .fetchResults()
                ;*/

        List<Member> resultList = queryFactory
                .selectFrom(member)
                .fetch();
    }

    /*
     * 정렬 순서
     * 1. 회원 나이 내림차순(Desc)
     * 2. 회원 이름 오름차순(Asc)
     * 단, 2에서 회원 이름이 없으면 마지막에 출력(nulls last)
     * */
    @Test
    void sort() {
        em.persist(new Member(null, 100));
        em.persist(new Member("member5", 100));
        em.persist(new Member("member6", 100));

        List<Member> memberList = queryFactory
                .selectFrom(member)
                .where(member.age.eq(100))
                .orderBy(member.age.desc(), member.username.asc().nullsLast())
                .fetch();

        for (Member member1 : memberList) {
            System.out.println("member1 = " + member1);
        }
    }

    // 페이징
    @Test
    void paging1() {
        List<Member> memberList = queryFactory
                .selectFrom(member)
                .orderBy(member.username.desc())
                .offset(1)
                .limit(2)
                .fetch();

        assertThat(memberList.size()).isEqualTo(2);
    }

    // 집합
    @Test
    void aggregation() {
        List<Tuple> result = queryFactory
                .select(
                        member.count(),
                        member.age.sum(),
                        member.age.avg(),
                        member.age.max(),
                        member.age.min()
                )
                .from(member)
                .fetch();

        Tuple tuple = result.get(0);
        assertThat(tuple.get(member.count())).isEqualTo(4);
        assertThat(tuple.get(member.age.sum())).isEqualTo(100);
    }

    /*
     * 팀의 이름과 각 팀의 평균 연령 구하기
     * */
    @Test
    void group() throws IOException {
        List<Tuple> fetch = queryFactory
                .select(team.name, member.age.avg())
                .from(member)
                .join(member.team, team)
                .groupBy(team.name)
                .fetch();

        for (Tuple tuple : fetch) {
            System.out.println("tuple = " + tuple);
        }
    }

    @Test
    void defaultJoin() {
        List<Member> result = queryFactory
                .selectFrom(member)
                .join(member.team, team)
                .where(team.name.eq("teamA"))
                .fetch();

        assertThat(result)
                .extracting("username")
                .containsExactly("member1", "member2")
        ;
    }

    /*
     * 예) 화원과 팀을 조인하면서, 팀 이름이 teamA인 팀만 조인, 회원은 모두 조회
     * */
    @Test
    void join_on_filtering() {
        List<Tuple> result = queryFactory
                .select(member, team)
                .from(member)
                .leftJoin(member.team, team)
                .on(team.name.eq("teamA"))
                .fetch();

        for (Tuple tuple : result) {
            System.out.println("tuple = " + tuple);
        }
    }

    @PersistenceUnit
    EntityManagerFactory emf;

    @Test
    void fetchJoinNoTest() {
        em.flush();
        em.clear();

        Member member = queryFactory
                .selectFrom(QMember.member)
                .where(QMember.member.username.eq("member1"))
                .fetchOne();

        // 지연 로딩에 따른 team 엔티티가 로딩이 되었는지 확인. 지연로딩 설정이 되어있기 때문에 false
        boolean loaded = emf.getPersistenceUnitUtil().isLoaded(member.getTeam());

        assertThat(loaded).isFalse();
    }

    @Test
    void fetchJoinUseTest() {
        em.flush();
        em.clear();

        Member result = queryFactory
                .selectFrom(member)
                .join(member.team, team).fetchJoin() /* fetchJoin 추가해주면 team 엔티티도 같이 조회 */
                .where(QMember.member.username.eq("member1"))
                .fetchOne();

        // 지연 로딩에 따른 team 엔티티가 로딩이 되었는지 확인. 지연로딩 설정이 되어있기 때문에 false
        boolean loaded = emf.getPersistenceUnitUtil().isLoaded(result.getTeam());

        assertThat(loaded).isTrue();
    }

    /**
     * 나이가 가장 많은 회원 조회
     * */
    @Test
    public void subQueryTest() {
        QMember memberSub = new QMember("memberSub");

        List<Member> result = queryFactory
                .selectFrom(member)
                .where(member.age.eq(
                        select(memberSub.age.max())
                                .from(memberSub)

                ))
                .fetch();
        assertThat(result).extracting("age")
                .containsExactly(40);
    }

    /**
     * 나이가 평균 이상인 회원 조회
     * */
    @Test
    public void subQueryGoeTest() {
        QMember memberSub = new QMember("memberSub");

        List<Member> result = queryFactory
                .selectFrom(member)
                .where(member.age.goe(
                        select(memberSub.age.avg())
                                .from(memberSub)

                ))
                .fetch();
        assertThat(result).extracting("age")
                .containsExactly(30, 40);
    }

    /**
     * 나이가 평균 이상인 회원 조회
     * */
    @Test
    public void subQueryInTest() {
        QMember memberSub = new QMember("memberSub");

        List<Member> result = queryFactory
                .selectFrom(member)
                .where(member.age.in(
                        select(memberSub.age)
                                .from(memberSub)
                                .where(member.age.gt(10))

                ))
                .fetch();
        assertThat(result).extracting("age")
                .containsExactly(20, 30, 40);
    }

    @Test
    public void selectSubQueryTest() {
        QMember memberSub = new QMember("memberSub");

        List<Tuple> result = queryFactory
                .select(member.username,
                        select(memberSub.age.avg())
                                .from(memberSub)
                )
                .from(member)
                .fetch();

        for (Tuple tuple : result) {
            System.out.println("tuple = " + tuple);
        }
    }

    @Test
    public void basicCase() {
        List<String> result = queryFactory
                .select(member.age
                        .when(10).then("ten")
                        .when(20).then("twenty")
                        .otherwise("etc")
                )
                .from(member)
                .fetch();

        for (String s : result) {
            System.out.println("s = " + s);
        }
    }

    @Test
    public void complexCase() {
        List<String> result = queryFactory
                .select(
                        new CaseBuilder()
                                .when(member.age.between(0, 20)).then("0 to 20")
                                .when(member.age.between(21, 30)).then("21 to 30")
                                .otherwise("etc")
                )
                .from(member)
                .fetch();

        for (String s : result) {
            System.out.println("s = " + s);
        }
    }

    @Test
    public void constant() {
        List<Tuple> result = queryFactory
                .select(member.username, Expressions.constant("A"))
                .from(member)
                .fetch();

        for (Tuple tuple : result) {
            System.out.println("tuple = " + tuple);
        }
    }

    @Test
    public void concatTest() {
        List<String> result = queryFactory
                .select(member.username.concat("_").concat(member.age.stringValue()))
                .from(member)
                .where(member.username.eq("member1"))
                .fetch();

        for (String tuple : result) {
            System.out.println("tuple = " + tuple);
        }
    }

    @Test
    public void simpleProjection() {
        List<String> result = queryFactory
                .select(member.username)
                .from(member)
                .fetch();

        for (String s : result) {
            System.out.println("s = " + s);
        }
    }

    @Test
    public void tupleProjection() {
        List<Tuple> result = queryFactory
                .select(member.username, member.age)
                .from(member)
                .fetch();

        for (Tuple tuple : result) {
            System.out.println("tuple.username = " + tuple.get(member.username));
            System.out.println("tuple.age = " + tuple.get(member.age));
        }
    }

    @Test
    public void findDtoBySetter() {
        List<SimpleMemberDto> result = queryFactory
                .select(Projections.bean(SimpleMemberDto.class, member.username, member.age))
                .from(member)
                .fetch();

        for (SimpleMemberDto simpleMemberDto : result) {
            System.out.println("simpleMemberDto = " + simpleMemberDto);
        }
    }

    @Test
    public void findDtoByField() {
        List<SimpleMemberDto> result = queryFactory
                .select(Projections.fields(SimpleMemberDto.class, member.username, member.age))
                .from(member)
                .fetch();

        for (SimpleMemberDto simpleMemberDto : result) {
            System.out.println("simpleMemberDto = " + simpleMemberDto);
        }
    }

    @Test
    public void findDtoByConstructor() {
        List<SimpleMemberDto> result = queryFactory
                .select(Projections.constructor(SimpleMemberDto.class, member.username, member.age))
                .from(member)
                .fetch();

        for (SimpleMemberDto simpleMemberDto : result) {
            System.out.println("simpleMemberDto = " + simpleMemberDto);
        }
    }

    @Test
    public void findUserDto() {
        QMember memberSub = new QMember("memberSub");

        List<UserDto> result = queryFactory
                .select(Projections.fields(UserDto.class
                        , member.username.as("name")
                        , ExpressionUtils.as(JPAExpressions
                                .select(memberSub.age.max())
                                .from(memberSub), "age")
                        )
                )
                .from(member)
                .fetch();

        for (UserDto userDto : result) {
            System.out.println("userDto = " + userDto);
        }
    }
}
