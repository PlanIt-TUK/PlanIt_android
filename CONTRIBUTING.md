커밋 구조

<type>(<scope>):<subject> -- 헤더
<BLANK LINE> -- 여백
<body> -- 본문
<BLANK LINE> -- 여백
<footer> -- 바닥글
 

구조 뜻 

type	
feat : 새로운 기능 추가
fix : 버그 수정
docs : 문서 관련
style : 스타일 변경 (포매팅 수정, 들여쓰기 추가, …)
refactor : 코드 리팩토링
test : 테스트 관련 코드
build : 빌드 관련 파일 수정
ci : CI 설정 파일 수정
perf : 성능 개선
chore : 그 외 자잘한 수정
scope (option)	변경되는 구역으로 함수명(괄호까지 적어서 구분), 파일명(확장자 적어서 구분) 등 변경될 수 있는 모든 영역
subject	명령문, 현재 시제로 마침표를 붙이지 않
body	명령문, 현재 시제로 어떻게 바꾼지 보다는 바꾼 의도를 적기
footer	git issue를 사용하여 작업 할 경우 해결된 issue에 대해서 다음 처럼 표현 close: #이슈번호 표현
 

예시

git commit -m "fix : 메인 페이지 접속 시 발생하는 콘솔 에러 수정

기존에 메인 페이지에서 중앙 이미지 슬라이드를 불러오는 코드에서
이전에 넣어둔 더미 코드로 인해 발생하는 콘솔 에러 제거

close: #1254"
