import os
import cv2
import numpy as np
import tensorflow as tf
import argparse

# ================================================================
# MediaPipe手検出モジュール
# ================================================================
class HandDetector:
    """MediaPipeを使用した手の検出"""

    def __init__(self):
        try:
            import mediapipe as mp
            from mediapipe.python.solutions import hands as mp_hands
            from mediapipe.python.solutions import drawing_utils as mp_drawing

            self.mp_hands = mp_hands.Hands(
                static_image_mode=True,
                max_num_hands=2,
                min_detection_confidence=0.5,
                min_tracking_confidence=0.5
            )
            self.mp_draw = mp_drawing
            self.hand_connections = mp_hands.HAND_CONNECTIONS
            print("✓ MediaPipe Hands を初期化")

        except Exception as e:
            print(f"エラー: {str(e)}")
            raise Exception("MediaPipeの初期化に失敗しました。'pip install mediapipe' を確認してください。")

    def detect(self, image):
        """画像から手を検出"""
        image_rgb = cv2.cvtColor(image, cv2.COLOR_BGR2RGB)
        results = self.mp_hands.process(image_rgb)

        hands_data = []
        if results.multi_hand_landmarks:
            h, w, _ = image.shape

            for hand_landmarks in results.multi_hand_landmarks:
                # ランドマーク座標を取得
                landmarks = []
                for lm in hand_landmarks.landmark:
                    landmarks.append({
                        'x': int(lm.x * w),
                        'y': int(lm.y * h),
                        'z': lm.z
                    })

                # バウンディングボックスを計算
                x_coords = [lm['x'] for lm in landmarks]
                y_coords = [lm['y'] for lm in landmarks]

                bbox = {
                    'x_min': max(0, min(x_coords) - 20),
                    'y_min': max(0, min(y_coords) - 20),
                    'x_max': min(w, max(x_coords) + 20),
                    'y_max': min(h, max(y_coords) + 20)
                }

                # 指先の座標（親指、人差し指、中指）
                finger_tips = [
                    landmarks[4],   # 親指
                    landmarks[8],   # 人差し指
                    landmarks[12],  # 中指
                ]

                hands_data.append({
                    'landmarks': landmarks,
                    'bbox': bbox,
                    'finger_tips': finger_tips,
                    'mp_landmarks': hand_landmarks
                })

        return hands_data

    def draw(self, image, hands_data):
        """手のランドマークを描画"""
        for hand in hands_data:
            # バウンディングボックス
            bbox = hand['bbox']
            cv2.rectangle(image,
                         (bbox['x_min'], bbox['y_min']),
                         (bbox['x_max'], bbox['y_max']),
                         (0, 255, 0), 2)

            # ランドマーク
            self.mp_draw.draw_landmarks(
                image,
                hand['mp_landmarks'],
                self.hand_connections
            )

        return image

# ================================================================
# ペン検出モジュール（YOLOv8/v11 推奨だが、既存のOpenCVベースを最適化）
# ================================================================
class PenDetector:
    """OpenCVを使用したペン検出（色と形状の最適化）"""

    def __init__(self):
        pass

    def detect(self, image):
        """形状と色ベースでペンを検出"""
        hsv = cv2.cvtColor(image, cv2.COLOR_BGR2HSV)

        # 黒・暗い色のペン検出（明度と彩度の範囲を調整）
        lower_dark = np.array([0, 0, 0])
        upper_dark = np.array([180, 255, 100])
        mask_dark = cv2.inRange(hsv, lower_dark, upper_dark)

        # 適応的二値化（照明変化に強い）
        gray = cv2.cvtColor(image, cv2.COLOR_BGR2GRAY)
        binary = cv2.adaptiveThreshold(gray, 255, cv2.ADAPTIVE_THRESH_GAUSSIAN_C, 
                                     cv2.THRESH_BINARY_INV, 11, 2)

        # マスクを組み合わせる
        combined_mask = cv2.bitwise_or(binary, mask_dark)

        # モルフォロジー演算でノイズ除去と形状の結合
        kernel = np.ones((5, 5), np.uint8)
        combined_mask = cv2.morphologyEx(combined_mask, cv2.MORPH_CLOSE, kernel)
        combined_mask = cv2.morphologyEx(combined_mask, cv2.MORPH_OPEN, kernel)

        # 輪郭検出
        contours, _ = cv2.findContours(combined_mask, cv2.RETR_EXTERNAL, cv2.CHAIN_APPROX_SIMPLE)

        pens = []
        for contour in contours:
            area = cv2.contourArea(contour)

            # 面積フィルター（画像サイズに応じて調整が必要な場合がある）
            if area < 200 or area > 50000:
                continue

            # 最小外接矩形
            rect = cv2.minAreaRect(contour)
            (x, y), (w, h), angle = rect
            
            if w > 0 and h > 0:
                width = min(w, h)
                height = max(w, h)
                aspect_ratio = height / width
            else:
                continue

            # 細長い形状をペンと判定（アスペクト比の閾値を調整）
            if aspect_ratio > 3.0:
                bx, by, bw, bh = cv2.boundingRect(contour)

                pens.append({
                    'bbox': {
                        'x_min': bx,
                        'y_min': by,
                        'x_max': bx + bw,
                        'y_max': by + bh
                    },
                    'center': {
                        'x': int(x),
                        'y': int(y)
                    },
                    'confidence': min(0.95, 0.4 + (aspect_ratio / 15.0)),
                    'area': area,
                    'aspect_ratio': aspect_ratio
                })

        # 信頼度でソート
        pens.sort(key=lambda x: x['confidence'], reverse=True)
        return pens[:5]

    def draw(self, image, pens):
        """ペンの検出結果を描画"""
        for pen in pens:
            bbox = pen['bbox']
            cv2.rectangle(image,
                         (bbox['x_min'], bbox['y_min']),
                         (bbox['x_max'], bbox['y_max']),
                         (255, 0, 0), 2)

            label = f"Pen {pen['confidence']:.2f}"
            cv2.putText(image, label,
                       (bbox['x_min'], bbox['y_min'] - 10),
                       cv2.FONT_HERSHEY_SIMPLEX, 0.5, (255, 0, 0), 2)
        return image

# ================================================================
# ペン保持判定ロジック
# ================================================================
class PenHoldingDetector:
    """手とペンの位置関係からペン保持を判定"""

    def __init__(self):
        self.hand_detector = HandDetector()
        self.pen_detector = PenDetector()

    def calculate_distance(self, point1, point2):
        """2点間の距離"""
        return np.sqrt((point1['x'] - point2['x'])**2 + (point1['y'] - point2['y'])**2)

    def is_holding(self, hand, pen, distance_threshold=120):
        """手がペンを持っているか判定"""
        pen_center = pen['center']
        bbox = hand['bbox']
        
        # 1. ペンが手のバウンディングボックス内にあるか（少し余裕を持たせる）
        margin = 30
        pen_in_hand = (
            bbox['x_min'] - margin <= pen_center['x'] <= bbox['x_max'] + margin and
            bbox['y_min'] - margin <= pen_center['y'] <= bbox['y_max'] + margin
        )

        # 2. 指先とペンの距離
        min_distance = float('inf')
        for finger_tip in hand['finger_tips']:
            dist = self.calculate_distance(finger_tip, pen_center)
            min_distance = min(min_distance, dist)

        # 判定：ボックス内にあるか、指先が十分近いか
        is_holding = pen_in_hand or min_distance < distance_threshold
        return is_holding, min_distance

    def detect(self, image):
        """画像からペン保持を検出"""
        hands = self.hand_detector.detect(image)
        pens = self.pen_detector.detect(image)

        results = {
            'holding_pen': False,
            'hands_count': len(hands),
            'pens_count': len(pens),
            'matches': []
        }

        for hand_idx, hand in enumerate(hands):
            for pen_idx, pen in enumerate(pens):
                is_holding, distance = self.is_holding(hand, pen)
                if is_holding:
                    results['holding_pen'] = True
                    results['matches'].append({
                        'hand_idx': hand_idx,
                        'pen_idx': pen_idx,
                        'distance': distance,
                        'confidence': pen['confidence']
                    })

        return results, hands, pens

    def visualize(self, image, results, hands, pens):
        """結果を可視化"""
        vis_image = image.copy()
        vis_image = self.hand_detector.draw(vis_image, hands)
        vis_image = self.pen_detector.draw(vis_image, pens)

        status = "Holding Pen: YES" if results['holding_pen'] else "Holding Pen: NO"
        color = (0, 255, 0) if results['holding_pen'] else (0, 0, 255)
        cv2.putText(vis_image, status, (10, 40), cv2.FONT_HERSHEY_SIMPLEX, 1.2, color, 3)

        info = f"Hands: {results['hands_count']} | Pens: {results['pens_count']}"
        cv2.putText(vis_image, info, (10, 80), cv2.FONT_HERSHEY_SIMPLEX, 0.7, (255, 255, 255), 2)
        return vis_image

# ================================================================
# TensorFlow Liteモデル作成（最適化版）
# ================================================================
class TFLiteModelBuilder:
    """判定ロジックをTensorFlow Liteモデルに変換"""

    def build_model(self, input_dim=10):
        """軽量な判定モデルを構築"""
        model = tf.keras.Sequential([
            tf.keras.layers.Input(shape=(input_dim,)),
            tf.keras.layers.Dense(16, activation='relu'),
            tf.keras.layers.Dense(8, activation='relu'),
            tf.keras.layers.Dense(2, activation='softmax')
        ])
        model.compile(optimizer='adam', loss='sparse_categorical_crossentropy', metrics=['accuracy'])
        return model

    def extract_features(self, hand, pen, img_shape=(1000, 1000)):
        """手とペンから特徴ベクトルを抽出（正規化）"""
        h, w = img_shape[:2]
        
        hand_center_x = (hand['bbox']['x_min'] + hand['bbox']['x_max']) / 2.0 / w
        hand_center_y = (hand['bbox']['y_min'] + hand['bbox']['y_max']) / 2.0 / h
        pen_center_x = pen['center']['x'] / w
        pen_center_y = pen['center']['y'] / h

        distance = np.sqrt((hand_center_x - pen_center_x)**2 + (hand_center_y - pen_center_y)**2)
        
        pen_in_hand = float(
            hand['bbox']['x_min']/w <= pen_center_x <= hand['bbox']['x_max']/w and
            hand['bbox']['y_min']/h <= pen_center_y <= hand['bbox']['y_max']/h
        )

        features = np.array([
            hand_center_x, hand_center_y,
            pen_center_x, pen_center_y,
            distance, pen_in_hand,
            (hand['bbox']['x_max'] - hand['bbox']['x_min']) / w,
            (hand['bbox']['y_max'] - hand['bbox']['y_min']) / h,
            (pen['bbox']['x_max'] - pen['bbox']['x_min']) / w,
            (pen['bbox']['y_max'] - pen['bbox']['y_min']) / h,
        ], dtype=np.float32)
        return features

    def train_and_convert(self, output_path='pen_holding_model.tflite'):
        """モデルを訓練してTFLiteに変換（INT8量子化対応）"""
        print("モデルを構築・訓練中...")
        model = self.build_model()
        
        # 合成データ生成
        X = []
        y = []
        for _ in range(3000):
            hx, hy = float(np.random.rand()), float(np.random.rand())
            hw, hh = float(np.random.uniform(0.1, 0.3)), float(np.random.uniform(0.1, 0.3))
            if np.random.rand() > 0.5: # Holding
                px, py = hx + float(np.random.uniform(-0.05, 0.05)), hy + float(np.random.uniform(-0.05, 0.05))
                label = 1
            else: # Not holding
                px, py = float(np.random.rand()), float(np.random.rand())
                label = 0
            dist = float(np.sqrt((hx-px)**2 + (hy-py)**2))
            pw, ph = float(np.random.uniform(0.01, 0.05)), float(np.random.uniform(0.1, 0.3))
            X.append([hx, hy, px, py, dist, float(dist < 0.1), hw, hh, pw, ph])
            y.append(label)
        
        X, y = np.array(X, dtype=np.float32), np.array(y, dtype=np.int32)
        model.fit(X, y, epochs=15, batch_size=32, verbose=0)

        print("TFLite形式に変換（最適化適用）...")
        converter = tf.lite.TFLiteConverter.from_keras_model(model)
        converter.optimizations = [tf.lite.Optimize.DEFAULT] # 動的範囲量子化
        tflite_model = converter.convert()

        # 16KB (16384 bytes) アライメント対応
        # Android 15以降の16KBページサイズ要件に対応するため、ファイルの末尾をパディングします
        alignment = 16384
        padding_size = (alignment - (len(tflite_model) % alignment)) % alignment
        aligned_model = tflite_model + b'\0' * padding_size

        with open(output_path, 'wb') as f:
            f.write(aligned_model)
        
        print(f"✓ TFLiteモデルを保存 (16KBアライメント済み): {output_path}")
        print(f"  サイズ: {len(aligned_model)/1024:.2f} KB (パディング: {padding_size} bytes)")
        return output_path

# ================================================================
# TFLite推論クラス
# ================================================================
class TFLitePredictor:
    def __init__(self, model_path):
        self.interpreter = tf.lite.Interpreter(model_path=model_path)
        self.interpreter.allocate_tensors()
        self.input_details = self.interpreter.get_input_details()
        self.output_details = self.interpreter.get_output_details()

    def predict(self, features):
        if len(features.shape) == 1:
            features = np.expand_dims(features, axis=0)
        self.interpreter.set_tensor(self.input_details[0]['index'], features.astype(np.float32))
        self.interpreter.invoke()
        output_data = self.interpreter.get_tensor(self.output_details[0]['index'])[0]
        prediction = np.argmax(output_data)
        return {
            'label': 'holding_pen' if prediction == 1 else 'no_pen',
            'confidence': float(output_data[prediction]),
            'probabilities': output_data.tolist()
        }

# ================================================================
# メイン処理
# ================================================================
def main():
    parser = argparse.ArgumentParser(description='Pen Holding Detection')
    parser.add_argument('--image', type=str, help='Path to input image')
    parser.add_argument('--train', action='store_true', help='Train and export TFLite model')
    args = parser.parse_args()

    if args.train:
        builder = TFLiteModelBuilder()
        builder.train_and_convert('pen_holding_classifier.tflite')
        with open('labels.txt', 'w') as f:
            f.write('no_pen\nholding_pen\n')
        print("✓ モデルとラベルを生成しました。")

    if args.image:
        if not os.path.exists(args.image):
            print(f"エラー: ファイルが見つかりません: {args.image}")
            return

        image = cv2.imread(args.image)
        detector = PenHoldingDetector()
        results, hands, pens = detector.detect(image)
        
        # 結果表示
        print(f"\n判定結果: {'ペンを持っています' if results['holding_pen'] else 'ペンを持っていません'}")
        print(f"検出数: 手={results['hands_count']}, ペン={results['pens_count']}")
        
        # 可視化保存
        vis_image = detector.visualize(image, results, hands, pens)
        cv2.imwrite('result.jpg', vis_image)
        print("✓ 可視化結果を 'result.jpg' に保存しました。")

        # TFLiteテスト（モデルが存在する場合）
        if os.path.exists('pen_holding_classifier.tflite') and hands and pens:
            predictor = TFLitePredictor('pen_holding_classifier.tflite')
            builder = TFLiteModelBuilder()
            features = builder.extract_features(hands[0], pens[0], image.shape)
            res = predictor.predict(features)
            print(f"TFLite予測: {res['label']} (信頼度: {res['confidence']:.2f})")

if __name__ == "__main__":
    main()
