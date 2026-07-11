package aaa.auth_p.service;

import aaa.auth_p.model.LoginDTO;
import aaa.auth_p.model.LoginResponseDTO;
import aaa.auth_p.model.RegisterDTO;
import aaa.carrier_p.model.CarrierMapper;
import aaa.driver_p.model.DriverDTO;
import aaa.driver_p.model.DriverMapper;
import aaa.filter.JwtUtil;
import aaa.user_p.model.UserDTO;
import aaa.user_p.model.UserMapper;
import aaa.vehicle_p.model.VehicleDTO;
import aaa.vehicle_p.model.VehicleMapper;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class AuthService {

    private final UserMapper mapper;
    private final CarrierMapper carrierMapper;
    private final DriverMapper driverMapper;
    private final VehicleMapper vehicleMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    public AuthService(
            UserMapper mapper,
            CarrierMapper carrierMapper,
            DriverMapper driverMapper,
            VehicleMapper vehicleMapper,
            PasswordEncoder passwordEncoder,
            JwtUtil jwtUtil
    ) {
        this.mapper = mapper;
        this.carrierMapper = carrierMapper;
        this.driverMapper = driverMapper;
        this.vehicleMapper = vehicleMapper;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
    }

    public LoginResponseDTO login(LoginDTO dto) {
        UserDTO user = mapper.findByLoginId(dto.getLoginId());

        if (user == null || !checkPassword(dto.getPassword(), user.getPassword())) {
            throw new RuntimeException("?꾩씠???먮뒗 鍮꾨?踰덊샇媛 ?쇱튂?섏? ?딆뒿?덈떎.");
        }

        if (dto.getRoleCode() != null
                && !dto.getRoleCode().isBlank()
                && !dto.getRoleCode().equals(user.getRoleCode())) {
            throw new RuntimeException("?좏깮??濡쒓렇???좏삎??怨꾩젙 沅뚰븳怨??ㅻ쫭?덈떎.");
        }

        validateLoginStatus(user);

        if ("DRIVER".equals(user.getRoleCode())) {
            validateDriverLogin(user);
        }

        mapper.updateLastLogin(user.getUserId());

        String token = jwtUtil.createToken(
                user.getLoginId(),
                user.getRoleCode()
        );

        LoginResponseDTO res = new LoginResponseDTO();
        res.setUserId(user.getUserId());
        res.setLoginId(user.getLoginId());
        res.setUserName(user.getUserName());
        res.setRoleCode(user.getRoleCode());
        res.setStatus(user.getStatus());

        /*
         * 湲곗〈 ?꾨줎?몄? origin/main ?묒そ ?명솚??
         * LoginResponseDTO?먮룄 token, accessToken, tokenType ?꾨뱶瑜?紐⑤몢 ?붾떎.
         */
        res.setToken(token);
        res.setAccessToken(token);
        res.setTokenType("Bearer");

        return res;
    }

    private void validateLoginStatus(UserDTO user) {
        if ("PENDING".equals(user.getStatus())) {
            if ("DRIVER".equals(user.getRoleCode())) {
                throw new RuntimeException("吏?뺥븳 ?댁넚?ъ쓽 媛???뱀씤 ??濡쒓렇?명븷 ???덉뒿?덈떎.");
            }

            if ("CARRIER".equals(user.getRoleCode())) {
                throw new RuntimeException("愿由ъ옄 ?뱀씤 ??濡쒓렇?명븷 ???덉뒿?덈떎.");
            }

            throw new RuntimeException("?뱀씤 ??濡쒓렇?명븷 ???덉뒿?덈떎.");
        }

        if ("CARRIER_APPROVED".equals(user.getStatus())) {
            throw new RuntimeException("관리자 최종 승인 후 로그인할 수 있습니다.");
        }

        if ("REJECTED".equals(user.getStatus())) {
            throw new RuntimeException("媛???먮뒗 ?뱀씤 ?붿껌??諛섎젮?섏뿀?듬땲??");
        }

        if (!"ACTIVE".equals(user.getStatus())) {
            throw new RuntimeException("濡쒓렇?명븷 ???녿뒗 怨꾩젙 ?곹깭?낅땲??");
        }
    }

    private void validateDriverLogin(UserDTO user) {
        DriverDTO driver = driverMapper.findByUserId(user.getUserId());

        if (driver == null) {
            throw new RuntimeException("湲곗궗 ?뺣낫瑜?李얠쓣 ???놁뒿?덈떎.");
        }

        if (!Boolean.TRUE.equals(driver.getIsRegistered())) {
            throw new RuntimeException("吏?뺥븳 ?댁넚?ъ쓽 媛???뱀씤 ??濡쒓렇?명븷 ???덉뒿?덈떎.");
        }

        if (!Boolean.TRUE.equals(driver.getCanEnter())) {
            throw new RuntimeException("관리자 최종 승인 후 로그인할 수 있습니다.");
        }
    }

    /**
     * 湲곗〈 ?됰Ц ?뚯뒪??怨꾩젙怨?BCrypt 怨꾩젙??紐⑤몢 泥섎━?쒕떎.
     * 湲곗〈 ?됰Ц 怨꾩젙??紐⑤몢 BCrypt濡??꾪솚???ㅼ뿉???됰Ц 鍮꾧탳 遺遺꾩쓣 ?쒓굅?섎뒗 寃껋씠 醫뗫떎.
     */
    private boolean checkPassword(String inputPassword, String savedPassword) {
        if (inputPassword == null || savedPassword == null) {
            return false;
        }

        if (savedPassword.startsWith("$2a$")
                || savedPassword.startsWith("$2b$")
                || savedPassword.startsWith("$2y$")) {
            return passwordEncoder.matches(inputPassword, savedPassword);
        }

        return inputPassword.equals(savedPassword);
    }

    @Transactional
    public int register(RegisterDTO dto) {
        validateRegister(dto);

        if (mapper.countByLoginId(dto.getLoginId()) > 0) {
            throw new RuntimeException("?대? ?ъ슜 以묒씤 ?꾩씠?붿엯?덈떎.");
        }

        /*
         * 愿???뚯씠釉??낅젰 ?꾩뿉 ??븷蹂??꾩닔媛믪쓣 寃?ы븳??
         * 以묎컙 INSERT ???덉쇅媛 諛쒖깮?섎뜑?쇰룄 @Transactional濡?濡ㅻ갚?쒕떎.
         */
        if ("CARRIER".equals(dto.getRoleCode())) {
            validateCarrierRegister(dto);
        }

        if ("DRIVER".equals(dto.getRoleCode())) {
            validateDriverRegister(dto);
        }

        dto.setPassword(passwordEncoder.encode(dto.getPassword()));
        dto.setStatus(
                "ADMIN".equals(dto.getRoleCode())
                        ? "ACTIVE"
                        : "PENDING"
        );

        int result = mapper.insertUser(dto);

        if ("CARRIER".equals(dto.getRoleCode())) {
            carrierMapper.insertFromRegister(dto);
        }

        if ("DRIVER".equals(dto.getRoleCode())) {
            /*
             * 湲곗궗 ?뚯썝媛????李⑤웾? ?앹꽦?섏? ?딅뒗??
             * 吏?뺥븳 ?댁넚?ш? ?뱀씤?????몃젅?쇰윭瑜?諛곗젙?쒕떎.
             */
            driverMapper.insertFromRegister(dto);
            insertDriverTractor(dto);
        }

        return result;
    }

    public List<UserDTO> findUsers() {
        List<UserDTO> users = mapper.findAll();

        if (users != null) {
            users.forEach(user -> user.setPassword(null));
        }

        return users;
    }

    /**
     * 愿由ъ옄 怨꾩젙 ?곹깭 蹂寃?
     *
     * 愿由ъ옄 ?붾㈃?먯꽌??
     * - ?댁넚??PENDING 怨꾩젙留?ACTIVE ?먮뒗 REJECTED 泥섎━
     * - 湲곗궗??理쒖쥌 ?뱀씤? VehicleService.updateApproval()?먯꽌 泥섎━
     */
    @Transactional
    public UserDTO updateStatus(Long userId, String status) {
        if (!List.of(
                "PENDING",
                "CARRIER_APPROVED",
                "ACTIVE",
                "REJECTED"
        ).contains(status)) {
            throw new RuntimeException("?곹깭媛믪씠 ?щ컮瑜댁? ?딆뒿?덈떎.");
        }

        UserDTO user = mapper.findById(userId);

        if (user == null) {
            throw new RuntimeException("?ъ슜?먮? 李얠쓣 ???놁뒿?덈떎.");
        }

        if ("ACTIVE".equals(status)) {
            approveRelatedAccount(user);
        }

        if ("REJECTED".equals(status)) {
            rejectRelatedAccount(user);
        }

        mapper.updateStatus(userId, status);

        UserDTO updatedUser = mapper.findById(userId);

        if (updatedUser != null) {
            updatedUser.setPassword(null);
        }

        return updatedUser;
    }

    private void approveRelatedAccount(UserDTO user) {
        if ("CARRIER".equals(user.getRoleCode())) {
            carrierMapper.updateStatusByUserId(
                    user.getUserId(),
                    "APPROVED"
            );
        }

        /*
         * 湲곗궗 理쒖쥌 ?뱀씤? ?ш린??泥섎━?섏? ?딅뒗??
         *
         * 湲곗궗 ?먮쫫:
         * PENDING
         * ???댁넚???뱀씤: CARRIER_APPROVED
         * ???몃젅?쇰윭 諛곗젙
         * ??愿由ъ옄 李⑤웾 理쒖쥌 ?뱀씤: ACTIVE
         */
        if ("DRIVER".equals(user.getRoleCode())
                && !"CARRIER_APPROVED".equals(user.getStatus())) {
            throw new RuntimeException(
                    "湲곗궗???댁넚???뱀씤怨??몃젅?쇰윭 諛곗젙 ??理쒖쥌 ?뱀씤?????덉뒿?덈떎."
            );
        }
    }

    private void rejectRelatedAccount(UserDTO user) {
        if ("CARRIER".equals(user.getRoleCode())) {
            carrierMapper.updateStatusByUserId(
                    user.getUserId(),
                    "REJECTED"
            );
        }

        if ("DRIVER".equals(user.getRoleCode())) {
            driverMapper.updateApprovalByUserId(
                    user.getUserId(),
                    false,
                    false
            );
        }
    }

    private void validateRegister(RegisterDTO dto) {
        if (dto.getLoginId() == null || dto.getLoginId().isBlank()) {
            throw new RuntimeException("?꾩씠?붾뒗 ?꾩닔?낅땲??");
        }

        if (dto.getPassword() == null || dto.getPassword().length() < 4) {
            throw new RuntimeException("鍮꾨?踰덊샇??4???댁긽 ?낅젰?섏꽭??");
        }

        if (dto.getUserName() == null || dto.getUserName().isBlank()) {
            throw new RuntimeException("?대쫫? ?꾩닔?낅땲??");
        }

        if (!List.of(
                "ADMIN",
                "CARRIER",
                "DRIVER"
        ).contains(dto.getRoleCode())) {
            throw new RuntimeException(
                    "??븷? ADMIN, CARRIER, DRIVER 以??섎굹?ъ빞 ?⑸땲??"
            );
        }
    }

    private void validateCarrierRegister(RegisterDTO dto) {
        if (dto.getCarrierName() == null
                || dto.getCarrierName().isBlank()) {
            throw new RuntimeException("?댁넚?щ챸? ?꾩닔?낅땲??");
        }

        if (dto.getManagerName() == null
                || dto.getManagerName().isBlank()) {
            throw new RuntimeException("?댁넚???대떦?먮챸? ?꾩닔?낅땲??");
        }

        if (dto.getCarrierContact() == null
                || dto.getCarrierContact().isBlank()) {
            throw new RuntimeException("?댁넚???곕씫泥섎뒗 ?꾩닔?낅땲??");
        }
    }

    private void validateDriverRegister(RegisterDTO dto) {
        if (dto.getDriverName() == null
                || dto.getDriverName().isBlank()) {
            throw new RuntimeException("湲곗궗紐낆? ?꾩닔?낅땲??");
        }

        if (dto.getDriverContact() == null
                || dto.getDriverContact().isBlank()) {
            throw new RuntimeException("湲곗궗 ?곕씫泥섎뒗 ?꾩닔?낅땲??");
        }

        if (dto.getCarrierId() == null) {
            throw new RuntimeException("?뚯냽 ?댁넚?щ뒗 ?꾩닔?낅땲??");
        }
    }

    private void insertDriverTractor(RegisterDTO dto) {
        if (dto.getPlateNumber() == null || dto.getPlateNumber().isBlank()) {
            throw new RuntimeException("湲곗궗 ?몃옓??李⑤웾踰덊샇???꾩닔?낅땲??");
        }

        if (dto.getTractorNo() == null || dto.getTractorNo().isBlank()) {
            throw new RuntimeException("?몃옓??踰덊샇???꾩닔?낅땲??");
        }

        if (vehicleMapper.findByPlateNumber(dto.getPlateNumber()) != null) {
            throw new RuntimeException("?대? ?깅줉??李⑤웾踰덊샇?낅땲??");
        }

        VehicleDTO vehicle = new VehicleDTO();
        vehicle.setPlateNumber(dto.getPlateNumber());
        vehicle.setVehicleType("TRACTOR");
        vehicle.setTonnage(dto.getTonnage());
        vehicle.setIsRegistered(false);
        vehicle.setVehicleStatus("승인대기");
        vehicle.setTractorNo(dto.getTractorNo());
        vehicle.setChassisNo(null);
        vehicle.setDriverId(dto.getDriverId());
        vehicle.setCarrierId(dto.getCarrierId());
        vehicle.setUserId(dto.getUserId());

        if (vehicle.getTonnage() == null || vehicle.getTonnage().isBlank()) {
            vehicle.setTonnage("25T");
        }

        vehicleMapper.insert(vehicle);
    }
}
