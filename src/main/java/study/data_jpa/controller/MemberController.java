package study.data_jpa.controller;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import study.data_jpa.dto.MemberDto;
import study.data_jpa.entity.Member;
import study.data_jpa.repository.MemberRepository;

import java.util.Optional;

@RestController
@RequiredArgsConstructor
public class MemberController {

    private final MemberRepository memberRepository;

    @GetMapping("/members/{id}")
    public String findMember(@PathVariable("id") Long id) {
        Member member = memberRepository.findById(id).get();
        return member.getUsername();
    }

    /* 도메인 클래스 컨버터. 권장하진 않음. 조회용으로만 사용 */
    @GetMapping("/members2/{id}")
    public String findMember2(@PathVariable("id") Member member) {
        return member.getUsername();
    }

    /*
    * http://localhost:8080/members 전체 데이터 조회
    * http://localhost:8080/members?page=0 첫 페이지 데이터만 조회
    * http://localhost:8080/members?page=0&size=3 첫 페이지 데이터 조회 및 페이지당 3건의 데이터 조회
    * http://localhost:8080/members?page=0&size=3&sort=id,desc 정렬 옵션(sort) 추가 가능
    * */
    @GetMapping("/members")
    public Page<MemberDto> list(@PageableDefault(size = 5) Pageable pageable) {
        Page<Member> page = memberRepository.findAll(pageable);

        // API 에서 결과값 반환할 때는 Page값 그대로 넘기지 말자. 엔티티를 외부에 노출시키면 안됨. DTO로 변환해서 처리해야 함
        // 엔티티를 수정하면 API 자체에서 오류가 발생할 수 있기 때문
        Page<MemberDto> pageMap = page.map(MemberDto::new);
        return pageMap;
    }

    @PostConstruct // Spring Applcation 이 올라올 때 한번 실행됨
    public void init() {
        //memberRepository.save(new Member("userA"));

        for (int i = 0; i < 100; i++) {
            memberRepository.save(new Member("user" + i, i));
        }
    }
}
