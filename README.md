# whatap-subject

- @SpringBootApplication이 수행하는 역할 
  - 어플리케이션의 시작점 
  - @ComponentScan 
    - 어플리케이션 내 패키지에서 Component(@Component, @Service, @Repository 등) 어노테이션이 붙은 클래스 스캐닝 활성화 
  - @SpringBootConfiguration 
    - 스프링 컨테이너에 Bean을 추가적으로 등록하는 기능 활성화 
    - Configuration 클래스를 추가적으로 import하는 기능 활성화 
    - @**Test 어노테이션을 사용할 경우, SpringBootConfiguration 검색됨 
  - @EnableAutoConfiguration 
    - 어플리케이션 의존성 분석 및 적절한 설정 자동 구성 
    - ex) spring-boot-starter-web 의존성 추가 시 자동으로 Tomcat, Spring MVC 설정됨
- ComponentScan이란 
  - Spring이 특정 패키지에서 Bean을 자동으로 검색하고 등록하도록 하는 어노테이션 
  - SpringBootApplication에 기본적으로 포함되어 있지만 다른 패키지에 있는 컴포넌트일 경우 명시적으로 기입해줘야 함 
  - ex) @Component, @Service, @Repository, @Controller
- @Autowired 동작 과정 
  1. 스프링 컨테이너가 빈(Bean) 생성
  2. @Autowired가 있는 필드 검색
  3. 해당 타입 또는 이름의 빈을 검색
  4. 검색된 빈 주입
- Spring Bean LifeCycle
  1. Bean 생성 - @Component, @Bean
  2. 의존성 주입 - @Autowired, @Value
  3. 초기화 콜백 - @PostConstruct
  4. Bean 사용
  5. 소멸 콜백 - @PreDestroy
  6. Bean 소멸 - 스프링 컨테이너 종료
- RestTemplate
  - Spring에서 RESTful API를 호출하기 위해 제공하는 HTTP 클라이언트
  - 주로 다른 서비스(API)와 통신할 때 사용하며 동기방식으로 동작 
- @OneToMany, @ManyToOne
  - JPA에서 엔티티간의 관계 매핑할 때 사용되는 어노테이션
  - @OneToMany
    - 1:N 관계를 나타냄
    - 하나의 엔티티가 여러 개의 다른 엔티티와의 관계를 가질 때 사용
    - ex) School : Student
  - @ManyToOne
    - N:1 관계를 나타냄
    - 여러 엔티티가 하나의 엔티티를 참조할 때 사용
    - 외래 키를 가진 엔티티로 연관 관계의 주인
    - ex) Student : School