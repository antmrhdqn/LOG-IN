package com.insider.login.member.dto;

import com.insider.login.commute.dto.CommuteDTO;
import com.insider.login.department.dto.DepartmentDTO;
import com.insider.login.position.dto.PositionDTO;
import lombok.*;

import java.time.LocalDate;
import java.util.List;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@ToString
public class MemberDTO {
    private int memberId;                   // 구성원 번호 (사번)
    private String name;                    // 구성원 이름
    private String password;                // 비밀번호
    private int departNo;                   // 부서 번호
    private String positionName;            // 직급명
    private LocalDate employedDate;         // 입사일
    private String address;                 // 주소
    private String phoneNo;                 // 전화 번호
    private String memberStatus;            // 현재 상태
    private String email;                   // 이메일
    private String memberRole;              // 권한
    private DepartmentDTO department;       // 부서
    private PositionDTO position;           // 직급
    private List<CommuteDTO> commuteList;   // 출퇴근 리스트
    private int transferredNo;              // 발령 번호
}