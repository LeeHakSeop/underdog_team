-- 화물차 번호판 사진 ZIP 기반 트랙터 번호판 교체
-- 기존 번호판을 키로 사용하므로 vehicle 및 문자열 저장 로그를 함께 갱신합니다.
BEGIN;

CREATE TEMP TABLE tractor_plate_map (
    old_plate VARCHAR(30) PRIMARY KEY,
    new_plate VARCHAR(30) UNIQUE NOT NULL
);

INSERT INTO tractor_plate_map (old_plate, new_plate) VALUES
('부산80바1003', '충남88바3907'),
('부산80바1007', '전남98사8117'),
('부산80바1010', '충북89아3101'),
('부산80바1013', '충남84사2301'),
('부산80바1017', '충남83아1874'),
('부산80바1020', '충남71바5585'),
('부산80바1023', '충북84아6951'),
('부산80바1027', '충남80바5562'),
('부산80바1030', '충북85아4554'),
('부산80바1033', '전남98사8567'),
('부산80바1037', '전남81바4334'),
('부산80바1040', '전남98사7311'),
('부산80바1043', '전북81사1416'),
('부산80바1047', '경북98아1343'),
('부산80바1050', '충북86아8810'),
('부산80바1053', '울산80아5393'),
('부산80바1057', '전남80바4262'),
('부산80바1060', '전남99사4342'),
('부산80바1063', '전북91사4160'),
('부산80바1067', '경북80아9341'),
('부산80바1070', '경북99아7786'),
('부산80바1073', '충남92바9092'),
('부산80바1077', '전남98바5120'),
('부산80바1080', '경북83아7111'),
('부산80바1083', '경북98아7509'),
('부산80바1087', '경북86아2937'),
('부산80바1090', '경북81아5931'),
('부산80바1093', '전남98사2407'),
('부산80바1097', '경북99아6711'),
('부산80바1100', '경북86아4004'),
('부산80바1103', '경북87아6440'),
('부산80바1107', '경남80아1094'),
('부산80바1110', '경북88자1268'),
('부산80바1113', '경북86아3250'),
('부산80바1117', '전남80바1435'),
('부산80바1120', '충남96자1107'),
('부산80바1123', '경북80아1767'),
('부산80바1127', '울산80아6655'),
('부산80바1130', '경북98아6722'),
('부산80바1133', '경남81사6625'),
('부산80바1137', '전북80사1454'),
('부산80바1140', '경북98사5169'),
('부산80바1143', '전북70바2376'),
('부산80바1147', '경북98사2385'),
('부산80바1150', '충북98바8239'),
('부산80바1153', '전남86바2923'),
('부산80바1157', '경북83아1108'),
('부산80바1160', '전남88바7016'),
('부산80바1163', '경북80아2078'),
('부산80바1167', '경북15바1964'),
('부산80바1170', '전남82바3210'),
('부산80바1173', '경북98사4005'),
('부산80바1177', '경남82사6764'),
('부산80바1180', '경북87바2556'),
('부산80바1183', '경북86아6904'),
('부산80바1187', '경북83아3618'),
('부산80바1190', '경남99바1405'),
('부산80바1193', '경남06모7926'),
('부산80바1197', '경북83아6588'),
('부산80바1200', '경남81사2115'),
('부산80바1203', '경남80아8058'),
('부산80바1207', '경북86아7250'),
('부산80바1210', '경남99바5062'),
('부산80바1213', '경남99바6032'),
('부산80바1217', '경북86아7937'),
('부산80바1220', '전남80바1456'),
('부산80바1223', '경북86아4696'),
('부산80바1227', '전남87바1578'),
('부산80바1230', '전북80사2201'),
('부산80바1233', '경북86아1528'),
('부산80바1237', '경북80아1748'),
('부산80바1240', '경북83아4720'),
('부산80바1243', '경남83아2198'),
('부산80바1247', '경남99사6378'),
('부산80바1250', '경남83아3386'),
('부산80바1253', '경북86아4267'),
('부산80바1257', '경북86아3155'),
('부산80바1260', '충남94사1276'),
('부산80바1263', '경북87바2600'),
('부산80바1267', '충남99사6423'),
('부산80바1270', '충북84사6202'),
('부산80바1273', '서울90바3154'),
('부산80바1277', '경남72바7315'),
('부산80바1280', '경남81사2122'),
('부산80바1283', '경남81아9287'),
('부산80바1287', '경북83아8580'),
('부산80바1290', '서울82바7357'),
('부산80바1293', '서울98바8417'),
('부산80바1297', '서울86바4851'),
('부산80바1300', '경남82아5540');

-- 번호판 문자열을 별도 저장하는 테이블이 존재할 때만 갱신
DO $$
BEGIN
    IF to_regclass('public.plate_recognition') IS NOT NULL THEN
        IF EXISTS (SELECT 1 FROM information_schema.columns
                   WHERE table_schema='public' AND table_name='plate_recognition'
                     AND column_name='recognized_plate') THEN
            UPDATE plate_recognition p
               SET recognized_plate = m.new_plate
              FROM tractor_plate_map m
             WHERE p.recognized_plate = m.old_plate;
        END IF;

        IF EXISTS (SELECT 1 FROM information_schema.columns
                   WHERE table_schema='public' AND table_name='plate_recognition'
                     AND column_name='manual_correction') THEN
            UPDATE plate_recognition p
               SET manual_correction = m.new_plate
              FROM tractor_plate_map m
             WHERE p.manual_correction = m.old_plate;
        END IF;
    END IF;

    IF to_regclass('public.exception_log') IS NOT NULL
       AND EXISTS (SELECT 1 FROM information_schema.columns
                   WHERE table_schema='public' AND table_name='exception_log'
                     AND column_name='plate_number') THEN
        UPDATE exception_log e
           SET plate_number = m.new_plate
          FROM tractor_plate_map m
         WHERE e.plate_number = m.old_plate;
    END IF;
END $$;

-- 기준 차량 테이블 갱신
UPDATE vehicle v
   SET plate_number = m.new_plate
  FROM tractor_plate_map m
 WHERE v.plate_number = m.old_plate
   AND UPPER(TRIM(v.vehicle_type)) IN ('트랙터', 'TRACTOR');

-- 트랙터 수와 교체 결과 검증
DO $$
DECLARE
    expected_count INTEGER := 90;
    changed_count INTEGER;
BEGIN
    SELECT COUNT(*) INTO changed_count
      FROM vehicle v
      JOIN tractor_plate_map m ON v.plate_number = m.new_plate
     WHERE UPPER(TRIM(v.vehicle_type)) IN ('트랙터', 'TRACTOR');

    IF changed_count <> expected_count THEN
        RAISE EXCEPTION '트랙터 번호판 교체 검증 실패: 기대 %, 실제 %',
                        expected_count, changed_count;
    END IF;
END $$;

COMMIT;