USE appdb;

-- 菜系数据
INSERT INTO cuisine (code, name, emoji, group_type, sort_order) VALUES
('su', '苏菜', '🦐', 'chinese', 1),
('lu', '鲁菜', '🐟', 'chinese', 2),
('chuan', '川菜', '🌶️', 'chinese', 3),
('yue', '粤菜', '🦆', 'chinese', 4),
('zhe', '浙菜', '🐠', 'chinese', 5),
('xiang', '湘菜', '🔥', 'chinese', 6),
('min', '闽菜', '🦀', 'chinese', 7),
('hui', '徽菜', '🐷', 'chinese', 8),
('japanese', '日式', '🍣', 'international', 9),
('korean', '韩式', '🥢', 'international', 10),
('italian', '意式', '🍝', 'international', 11),
('french', '法式', '🥖', 'international', 12),
('indian', '印度', '🍛', 'international', 13),
('thai', '泰式', '🌶️', 'international', 14),
('mexican', '墨西哥', '🌮', 'international', 15)
ON DUPLICATE KEY UPDATE name=VALUES(name);

-- 食材分类
INSERT INTO ingredient_category (name, emoji, sort_order) VALUES
('荤菜', '🥩', 1),
('海鲜', '🐟', 2),
('蔬菜', '🥬', 3),
('菌菇', '🍄', 4),
('豆类', '🫘', 5),
('蛋类', '🥚', 6),
('水果', '🍎', 7),
('坚果', '🥜', 8),
('奶制品', '🥛', 9)
ON DUPLICATE KEY UPDATE emoji=VALUES(emoji);

-- 荤菜
INSERT INTO ingredient (category_id, name, sort_order)
SELECT c.id, v.name, v.sort_order FROM ingredient_category c
JOIN (
    SELECT '猪肉' name, 1 sort_order UNION SELECT '牛肉',2 UNION SELECT '羊肉',3 UNION SELECT '鸡肉',4
    UNION SELECT '鸭肉',5 UNION SELECT '鹅肉',6 UNION SELECT '兔肉',7 UNION SELECT '驴肉',8
    UNION SELECT '猪排骨',9 UNION SELECT '牛排',10 UNION SELECT '羊排',11 UNION SELECT '鸡翅',12
    UNION SELECT '鸡腿',13 UNION SELECT '鸡胸肉',14 UNION SELECT '鸡爪',15 UNION SELECT '鸭腿',16
    UNION SELECT '五花肉',17 UNION SELECT '瘦肉',18 UNION SELECT '肉丝',19 UNION SELECT '肉片',20
    UNION SELECT '肉丁',21 UNION SELECT '肉馅',22 UNION SELECT '里脊肉',23 UNION SELECT '梅花肉',24
    UNION SELECT '腊肉',25 UNION SELECT '香肠',26 UNION SELECT '火腿',27 UNION SELECT '培根',28
    UNION SELECT '腊肠',29 UNION SELECT '咸肉',30 UNION SELECT '风干肉',31 UNION SELECT '牛肉干',32
) v ON c.name='荤菜';

-- 海鲜
INSERT INTO ingredient (category_id, name, sort_order)
SELECT c.id, v.name, v.sort_order FROM ingredient_category c
JOIN (
    SELECT '鲈鱼' name, 1 sort_order UNION SELECT '鲫鱼',2 UNION SELECT '草鱼',3 UNION SELECT '鲤鱼',4
    UNION SELECT '带鱼',5 UNION SELECT '黄鱼',6 UNION SELECT '鳕鱼',7 UNION SELECT '三文鱼',8
    UNION SELECT '金枪鱼',9 UNION SELECT '鲳鱼',10 UNION SELECT '石斑鱼',11 UNION SELECT '桂鱼',12
    UNION SELECT '鲢鱼',13 UNION SELECT '青鱼',14 UNION SELECT '武昌鱼',15 UNION SELECT '比目鱼',16
    UNION SELECT '鳗鱼',17 UNION SELECT '刀鱼',18 UNION SELECT '大虾',19 UNION SELECT '基围虾',20
    UNION SELECT '龙虾',21 UNION SELECT '白虾',22 UNION SELECT '河虾',23 UNION SELECT '明虾',24
    UNION SELECT '对虾',25 UNION SELECT '皮皮虾',26 UNION SELECT '螃蟹',27 UNION SELECT '大闸蟹',28
    UNION SELECT '梭子蟹',29 UNION SELECT '青蟹',30 UNION SELECT '毛蟹',31 UNION SELECT '扇贝',32
) v ON c.name='海鲜';

-- 蔬菜
INSERT INTO ingredient (category_id, name, sort_order)
SELECT c.id, v.name, v.sort_order FROM ingredient_category c
JOIN (
    SELECT '白菜' name, 1 sort_order UNION SELECT '大白菜',2 UNION SELECT '小白菜',3 UNION SELECT '娃娃菜',4
    UNION SELECT '菠菜',5 UNION SELECT '生菜',6 UNION SELECT '油菜',7 UNION SELECT '空心菜',8
    UNION SELECT '苋菜',9 UNION SELECT '芥蓝',10 UNION SELECT '菜心',11 UNION SELECT '西兰花',12
    UNION SELECT '花菜',13 UNION SELECT '韭菜',14 UNION SELECT '韭黄',15 UNION SELECT '芹菜',16
    UNION SELECT '西芹',17 UNION SELECT '西红柿',18 UNION SELECT '樱桃番茄',19 UNION SELECT '黄瓜',20
    UNION SELECT '茄子',21 UNION SELECT '豆角',22 UNION SELECT '四季豆',23 UNION SELECT '荷兰豆',24
    UNION SELECT '豌豆',25 UNION SELECT '毛豆',26 UNION SELECT '青椒',27 UNION SELECT '红椒',28
    UNION SELECT '土豆',29 UNION SELECT '胡萝卜',30 UNION SELECT '白萝卜',31 UNION SELECT '莲藕',32
) v ON c.name='蔬菜';

-- 菌菇
INSERT INTO ingredient (category_id, name, sort_order)
SELECT c.id, v.name, v.sort_order FROM ingredient_category c
JOIN (
    SELECT '香菇' name, 1 sort_order UNION SELECT '花菇',2 UNION SELECT '平菇',3 UNION SELECT '金针菇',4
    UNION SELECT '杏鲍菇',5 UNION SELECT '口蘑',6 UNION SELECT '草菇',7 UNION SELECT '茶树菇',8
    UNION SELECT '猴头菇',9 UNION SELECT '竹荪',10 UNION SELECT '木耳',11 UNION SELECT '银耳',12
    UNION SELECT '白木耳',13 UNION SELECT '黑耳',14 UNION SELECT '云耳',15 UNION SELECT '毛木耳',16
) v ON c.name='菌菇';

-- 豆类
INSERT INTO ingredient (category_id, name, sort_order)
SELECT c.id, v.name, v.sort_order FROM ingredient_category c
JOIN (
    SELECT '豆腐' name, 1 sort_order UNION SELECT '嫩豆腐',2 UNION SELECT '老豆腐',3 UNION SELECT '内酯豆腐',4
    UNION SELECT '豆腐干',5 UNION SELECT '豆腐皮',6 UNION SELECT '腐竹',7 UNION SELECT '千张',8
    UNION SELECT '豆泡',9 UNION SELECT '豆芽',10 UNION SELECT '绿豆',11 UNION SELECT '红豆',12
    UNION SELECT '黑豆',13 UNION SELECT '蚕豆',14 UNION SELECT '黄豆',15
) v ON c.name='豆类';

-- 蛋类
INSERT INTO ingredient (category_id, name, sort_order)
SELECT c.id, v.name, v.sort_order FROM ingredient_category c
JOIN (
    SELECT '鸡蛋' name, 1 sort_order UNION SELECT '土鸡蛋',2 UNION SELECT '鸭蛋',3 UNION SELECT '鹅蛋',4
    UNION SELECT '鹌鹑蛋',5 UNION SELECT '咸鸭蛋',6 UNION SELECT '松花蛋',7
) v ON c.name='蛋类';

-- 水果
INSERT INTO ingredient (category_id, name, sort_order)
SELECT c.id, v.name, v.sort_order FROM ingredient_category c
JOIN (
    SELECT '苹果' name, 1 sort_order UNION SELECT '梨',2 UNION SELECT '桃子',3 UNION SELECT '李子',4
    UNION SELECT '杏',5 UNION SELECT '樱桃',6 UNION SELECT '葡萄',7 UNION SELECT '草莓',8
    UNION SELECT '蓝莓',9 UNION SELECT '芒果',10 UNION SELECT '香蕉',11 UNION SELECT '橙子',12
) v ON c.name='水果';

-- 坚果
INSERT INTO ingredient (category_id, name, sort_order)
SELECT c.id, v.name, v.sort_order FROM ingredient_category c
JOIN (
    SELECT '花生' name, 1 sort_order UNION SELECT '核桃',2 UNION SELECT '杏仁',3 UNION SELECT '腰果',4
    UNION SELECT '开心果',5 UNION SELECT '松子',6 UNION SELECT '榛子',7 UNION SELECT '栗子',8
    UNION SELECT '瓜子',9 UNION SELECT '南瓜子',10 UNION SELECT '芝麻',11 UNION SELECT '黑芝麻',12
) v ON c.name='坚果';

-- 奶制品
INSERT INTO ingredient (category_id, name, sort_order)
SELECT c.id, v.name, v.sort_order FROM ingredient_category c
JOIN (
    SELECT '牛奶' name, 1 sort_order UNION SELECT '酸奶',2 UNION SELECT '奶酪',3 UNION SELECT '奶油',4
    UNION SELECT '黄油',5 UNION SELECT '炼乳',6 UNION SELECT '奶粉',7 UNION SELECT '马苏里拉',8
) v ON c.name='奶制品';
