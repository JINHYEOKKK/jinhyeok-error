package com.togedog.member.service;

import com.togedog.auth.utils.CustomAuthorityUtils;
import com.togedog.exception.BusinessLogicException;
import com.togedog.exception.ExceptionCode;
import com.togedog.friend.repository.FriendRepository;
import com.togedog.friend.service.FriendService;
import com.togedog.member.entity.Member;
import com.togedog.member.repository.MemberRepository;
import com.togedog.pet.entity.Pet;
import com.togedog.pet.repository.PetRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class MemberService {
    private final MemberRepository memberRepository;
    private PetRepository petRepository;
    private final PasswordEncoder passwordEncoder;
    private final CustomAuthorityUtils authorityUtils;

    public Member createMember(Member member) {
        verifyExistMember(member.getEmail());
        verifyExistPhone(member.getPhone());
        verifyExistNickName(member.getNickName());

        String encryptedPassword = passwordEncoder.encode(member.getPassword());
        member.setPassword(encryptedPassword);
        List<String> roles = authorityUtils.createRoles(member.getEmail());
        member.setRoles(roles);

        Member verifiedMember = memberRepository.save(member);

        return verifiedMember;
    }

    public Member findMember(Authentication authentication) {
        Member member = extractMemberFromAuthentication(authentication);
        return member;
    }

    public Page<Member> findMembers(int page, int size, Authentication authentication) {
        return memberRepository.findAll(PageRequest.of(page, size,
                Sort.by("memberId").descending()));
    }

    public Member updateMember(Member member, Authentication authentication) {
        Member authenticatedMember = extractMemberFromAuthentication(authentication);

//        Member verifedMember = verifiedMember(member.getMemberId());

        Optional.ofNullable(member.getPhone())
                .ifPresent(phone -> authenticatedMember.setPhone(phone));
        Optional.ofNullable(member.getPassword())
                .ifPresent(password -> authenticatedMember.setPassword(password));
        Optional.ofNullable(member.getProfileImage())
                .ifPresent(profileImage -> authenticatedMember.setProfileImage(profileImage));
        Optional.ofNullable(member.getNickName())
                .ifPresent(nickName -> authenticatedMember.setNickName(nickName));

        return memberRepository.save(authenticatedMember);
    }

    public void deleteMember(Authentication authentication) {
        Member authenticatedMember = extractMemberFromAuthentication(authentication);
        authenticatedMember.setStatus(Member.memberStatus.DELETED);
        memberRepository.save(authenticatedMember);
    }

    private Member verifiedMember(long memberId) {
        Optional<Member> member = memberRepository.findById(memberId);

        Member verfiedMember =
                member.orElseThrow(() ->
                        new BusinessLogicException(ExceptionCode.MEMBER_NOT_FOUND));
        return verfiedMember;
    }

    private void verifyExistMember(String email) {
        Optional<Member> member = memberRepository.findByEmail(email);
            if(member.isPresent()) {
                    throw new BusinessLogicException(ExceptionCode.MEMBER_EXISTS);
            }
    }

    private void verifyExistPhone(String phone) {
        Optional<Member> member = memberRepository.findByPhone(phone);
        if(member.isPresent()) {
            throw new BusinessLogicException(ExceptionCode.PHONE_EXISTS);
        }
    }

    private void verifyExistNickName(String nickName) {
        Optional<Member> member = memberRepository.findByNickName(nickName);
        if(member.isPresent()) {
            throw new BusinessLogicException(ExceptionCode.NICKNAME_EXISTS);
        }
    }

    private Member extractMemberFromAuthentication(Authentication authentication) {
        /**
         * 첫 번째 if 블록에서는 메서드로 전달된 authentication 객체가 null인 경우,
         * SecurityContextHolder에서 인증 정보를 가져오려고 시도.
         * 두 번째 if 블록에서는 authentication이 여전히 null인 경우,
         * 사용자에게 인증되지 않았음을 알리고, 처리할 수 있도록 예외를 발생시킴.
         */
//        if (authentication == null) {
//            authentication = SecurityContextHolder.getContext().getAuthentication();
//        }
//
//        if (authentication == null) {
//            throw new BusinessLogicException(ExceptionCode.MEMBER_NOT_FOUND);
//        }

        String username = (String) authentication.getPrincipal();
        return memberRepository.findByEmail(username)
                .orElseThrow(() -> new BusinessLogicException(ExceptionCode.MEMBER_NOT_FOUND));
    }

}
