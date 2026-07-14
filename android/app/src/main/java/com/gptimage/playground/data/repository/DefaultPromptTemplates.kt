package com.gptimage.playground.data.repository

import com.gptimage.playground.data.model.PromptTemplate
import com.gptimage.playground.data.model.PromptTemplateCategory

/**
 * 内置提示词模板预设。与 Web 版 `default-prompt-templates.ts` 对齐：16 个分类。
 * 每个分类精选 4-6 条常用模板（Web 版共 154 条，这里移植 ~70 条覆盖核心场景，
 * 其余模板后续可通过 JSON 导入或后台维护追加）。
 *
 * 全部为中文数据，prompt 内容为可直接喂给图像模型的中文生图指令。
 */
object DefaultPromptTemplates {

    // ============ 分类（按 sortOrder 排序）============
    val categories: List<PromptTemplateCategory> = listOf(
        cat("style-transfer", "风格转换", "把现有图片转为不同艺术风格", sortOrder = 1),
        cat("ecommerce-product", "电商商品图", "商品主图、场景图、卖点图", sortOrder = 2),
        cat("social-media", "社交媒体内容", "封面、卡片、短视频素材", sortOrder = 3),
        cat("brand-marketing", "品牌营销", "主视觉、海报、营销战役", sortOrder = 4),
        cat("food-beverage", "餐饮美食", "菜单、外卖、广告图", sortOrder = 5),
        cat("fashion-beauty", "时尚美妆", "穿搭、护肤品、Lookbook", sortOrder = 6),
        cat("real-estate-interior", "地产空间", "客厅、酒店、办公室", sortOrder = 7),
        cat("education-training", "教育培训", "课程封面、知识地图、儿童认知卡", sortOrder = 8),
        cat("game-concept", "游戏概念", "角色设定、奇幻场景、科幻关卡", sortOrder = 9),
        cat("tech-ui", "科技产品", "SaaS 首屏、AI 助手、数据仪表盘", sortOrder = 10),
        cat("travel-culture", "旅行文旅", "目的地海报、城市名片、度假酒店", sortOrder = 11),
        cat("health-wellness", "健康生活", "瑜伽、健身、冥想空间", sortOrder = 12),
        cat("portrait-avatar", "头像人像", "职业头像、社交头像、角色头像", sortOrder = 13),
        cat("business-office", "商务办公", "报告封面、战略图、团队协作", sortOrder = 14),
        cat("seasonal-festival", "节日季节", "春夏秋冬季节性视觉", sortOrder = 15),
        cat("texture-background", "纹理背景", "渐变、大理石、纸张颗粒", sortOrder = 16)
    )

    // ============ 模板（按 categoryId 分组，按 sortOrder 排序）============
    val templates: List<PromptTemplate> = buildList {
        // style-transfer
        addAll(listOf(
            tpl("style-transfer-watercolor", "水彩插画", "style-transfer",
                "柔和纸张纹理、透明水彩边缘和轻盈色彩。",
                "将这张图片转换为精致的水彩插画风格，保留主体构图与关键细节。采用柔和纸张纹理、透明水彩边缘和轻盈色彩，让画面带有手绘的温度感。整体色调控制在 3-5 种主色之间，避免过饱和，留白处理保留水彩的呼吸感。"),
            tpl("style-transfer-anime", "日系动画", "style-transfer",
                "干净线稿、明亮配色和动画电影质感。",
                "将这张图片转换为日系动画风格，参考吉卜力或新海诚的画面语言。使用干净利落的线稿、明亮清透的配色、富有层次的云朵与天空，整体氛围温暖而充满叙事感。保留人物/主体的表情与姿态细节。"),
            tpl("style-transfer-cinema-poster", "电影海报", "style-transfer",
                "戏剧光影、胶片色彩和海报级构图。",
                "将这张图片转换为电影海报风格，强化戏剧光影、胶片颗粒感和高对比配色。构图上保留主体居中或三分之一构图，添加电影字幕区域。整体色调偏冷或暖金，营造大片氛围。"),
            tpl("style-transfer-oil-painting", "油画质感", "style-transfer",
                "古典油画笔触、厚重质感和暖色调。",
                "将这张图片转换为古典油画风格，参考文艺复兴或巴洛克油画语言。厚重笔触、明暗对比强烈（伦勃朗光）、暖色调主导，背景压暗以突出主体。保留人物皮肤的真实质感与衣物褶皱。"),
            tpl("style-transfer-pixel-art", "像素艺术", "style-transfer",
                "8-bit / 16-bit 像素风格，复古游戏感。",
                "将这张图片转换为 16-bit 像素艺术风格，参考 SNES 时代的复古游戏画面。采用限定调色板（32-64 色）、清晰的像素网格、抖动（dithering）阴影。整体保持简洁可读，适合游戏精灵或场景贴图。"),
            tpl("style-transfer-ink-wash", "中国水墨", "style-transfer",
                "水墨晕染、留白意境和东方韵味。",
                "将这张图片转换为中国水墨画风格，强调笔墨晕染、浓淡变化和大量留白。主体用浓墨勾勒轮廓，远景用淡墨晕染。可点缀少量淡彩（如花青、赭石）。整体追求意境而非写实，参考宋元山水或写意花鸟。")
        ))

        // ecommerce-product
        addAll(listOf(
            tpl("ecommerce-pure-white", "纯白主图", "ecommerce-product",
                "干净背景、柔和阴影和平台商品主图感。",
                "为这件商品生成一张纯白背景的电商主图，参考淘宝/京东主图标准。背景纯白 (#FFFFFF)，商品居中，柔和地面阴影体现立体感。灯光均匀、色彩还原准确、无明显反光。适合平台直投主图位。"),
            tpl("ecommerce-lifestyle", "生活方式场景", "ecommerce-product",
                "把商品自然放进使用场景，增强购买想象。",
                "为这件商品生成一张生活方式场景图，把商品自然融入使用场景（如厨房、客厅、户外）。环境光线自然、配色协调，突出商品的使用情境与情感价值，让用户产生代入感。商品占比 30-40%，不要喧宾夺主。"),
            tpl("ecommerce-floating-points", "悬浮卖点图", "ecommerce-product",
                "悬浮构图、卖点分层和高冲击展示。",
                "为这件商品生成一张悬浮卖点图，商品悬浮于画面中央，周围环绕 3-4 个卖点图标（带文字），用虚线或光晕连接。背景用品牌渐变色或科技感深色，突出科技感与高级感。适合电商详情页头图。"),
            tpl("ecommerce-360-rotating", "360 度展示", "ecommerce-product",
                "商品多角度展示，环形光带。",
                "为这件商品生成一张 360 度展示主视觉，商品居中略偏上，下方有半透明圆形旋转台。环形柔光灯带围绕商品，背景为深灰色渐变。突出商品的全维度立体感和质感。"),
            tpl("ecommerce-gift-box", "礼盒包装", "ecommerce-product",
                "节日礼盒、精致包装和高级氛围。",
                "为这件商品生成一张节日礼盒包装图，商品置于精美礼盒中或旁边。背景使用节日氛围色（圣诞红绿、新年金红、情人节粉金）。添加丝带、装饰花、灯光星点等元素，突出礼赠感和高级感。")
        ))

        // social-media
        addAll(listOf(
            tpl("social-xiaohongshu-cover", "小红书封面", "social-media",
                "清爽标题区、生活方式氛围和强点击感。",
                "生成一张小红书封面图，画面比例 3:4。顶部或左侧留出标题区（实际文字稍后添加），主体内容居中偏下。配色清爽明亮，参考生活方式博主的视觉风格。整体留白舒适，强点击感，适合干货/穿搭/美食类内容。"),
            tpl("social-short-video-cover", "短视频封面", "social-media",
                "强主体、大对比和移动端可读性。",
                "生成一张短视频封面图，画面比例 9:16。强主体居中，背景与主体形成高对比，确保移动端小尺寸下依然清晰可读。可在顶部或底部预留 1 行标题位。整体冲击力强，适合抖音/视频号。"),
            tpl("social-carousel-card", "知识轮播卡片", "social-media",
                "适合多页轮播的信息卡风格。",
                "生成一张知识轮播卡片背景图，画面比例 1:1。中央留出大块文字区域（实际文字稍后添加），四周用图标、装饰元素点缀。配色专业克制，参考得到/混沌大学的知识卡片风格。适合知识科普、方法论分享。"),
            tpl("social-instagram-grid", "Instagram 九宫格", "social-media",
                "九宫格统一视觉、品牌一致性。",
                "生成一张 Instagram 九宫格主视觉，每格独立可看但拼起来是统一画面。主体居中、四周延伸出装饰元素或副场景。配色统一，品牌识别度高。适合品牌账号的视觉统一管理。"),
            tpl("social-wechat-article-header", "公众号文章头图", "social-media",
                "强主题、留白和品牌感。",
                "生成一张公众号文章头图，画面比例 2.35:1。主体居中偏左，右侧留出标题文字位。配色与文章主题呼应，整体专业但不呆板。适合干货、行业观察、人物专访类文章。")
        ))

        // brand-marketing
        addAll(listOf(
            tpl("brand-key-visual", "品牌主视觉 KV", "brand-marketing",
                "适合新品、活动和官网首屏。",
                "为该品牌生成一张主视觉 KV，画面比例 16:9。中央留出 logo 与主标题位（实际文字稍后添加），四周用品牌主色 + 装饰元素延展。整体高级、有张力，适合新品发布、活动主视觉、官网首屏。"),
            tpl("brand-launch-poster", "新品发布海报", "brand-marketing",
                "科技感、期待感和发布会氛围。",
                "生成一张新品发布海报，参考苹果/华为发布会风格。深色背景 + 单点光源、产品悬浮居中、戏剧化光影。背景可加流光、粒子、星轨等元素，突出科技感与期待感。适合手机/数码产品发布。"),
            tpl("brand-campaign-concept", "营销战役概念", "brand-marketing",
                "为主题活动生成统一视觉方向。",
                "生成一张营销战役主视觉，主题：{活动主题}。画面要有统一视觉语言（重复元素、色彩系统、节奏感），适合跨渠道延展（海报、Banner、社交图）。强调情绪感染力与记忆点。"),
            tpl("brand-event-backdrop", "活动主视觉背景板", "brand-marketing",
                "大型发布会、年会、展会的背景板。",
                "生成一张活动主视觉背景板，画面比例 16:9。中央留出大量留白（用于现场主舞台/讲者位置），四周用品牌元素延展。整体大气、有仪式感，适合大型发布会、年会、峰会。")
        ))

        // food-beverage
        addAll(listOf(
            tpl("food-menu-main", "菜单主图", "food-beverage",
                "突出招牌菜和餐厅风格。",
                "为这道菜生成一张菜单主图，俯拍或 45 度斜拍。木质或大理石餐桌背景，搭配餐具与点缀食材。光线柔和自然，色彩饱满有食欲感。突出菜品的色香味形，适合高端餐厅菜单与外卖平台。"),
            tpl("food-delivery-clean", "外卖商品图", "food-beverage",
                "清晰、真实、食欲感强。",
                "为这道菜生成一张外卖商品图，纯白背景 + 柔和阴影。菜品居中，俯拍角度。色彩还原真实，不夸张饱和。突出食材新鲜度与成品诱惑力，适合美团/饿了么主图位。"),
            tpl("food-iced-drink", "冰饮清爽感", "food-beverage",
                "水珠、冰块和夏日清凉氛围。",
                "生成一张冰饮广告图，玻璃杯外壁布满水珠，杯中冰块晶莹剔透。背景为夏日阳光透过的氛围，可加柠檬/薄荷点缀。色调偏冷蓝与亮黄对比，突出清凉解渴感。"),
            tpl("food-hotpot-steamy", "火锅热气腾腾", "food-beverage",
                "热气、红油和聚餐氛围。",
                "生成一张火锅场景图，铜锅/铁锅中央，红油翻滚冒热气。桌上铺满新鲜食材（肉片、毛肚、蔬菜）。背景为中国传统餐厅氛围，灯笼、木质桌椅。突出聚餐热闹与食物的诱人热气。"),
            tpl("food-bakery-cozy", "烘焙温馨", "food-beverage",
                "暖光、面包屑和家的味道。",
                "生成一张烘焙场景图，刚出炉的面包/蛋糕居中。木桌、麻布、面包屑点缀。光线为暖黄色窗光，整体温馨、有家的味道。适合烘焙店、咖啡店宣传。")
        ))

        // fashion-beauty
        addAll(listOf(
            tpl("fashion-street-style", "街头穿搭", "fashion-beauty",
                "城市背景、自然姿态和潮流感。",
                "生成一张街头穿搭图，模特自然行走于城市街道。背景为都市街景（ Tokyo / Seoul / Paris 风格任选）。穿搭潮流但不夸张，姿势抓拍自然。光线为下午侧光，色彩偏胶片质感。"),
            tpl("fashion-lookbook", "Lookbook 画册", "fashion-beauty",
                "高级留白、统一色调和服装展示。",
                "生成一张 Lookbook 风格图，模特站立姿势简洁。纯色或低饱和背景，留白充足。整组色调统一（如全米色、全灰调）。突出服装的剪裁、面料与设计语言，适合品牌画册。"),
            tpl("beauty-skincare-water", "护肤品展示图", "fashion-beauty",
                "水润质感、成分氛围和洁净背景。",
                "生成一张护肤品广告图，瓶身居中，周围点缀水珠、绿叶、花瓣等成分元素。背景为洁净浅色（白/淡绿/淡蓝）。光线通透，突出产品的水润感与天然成分感。"),
            tpl("beauty-makeup-macro", "美妆特写", "fashion-beauty",
                "唇部/眼妆特写，色彩浓郁。",
                "生成一张美妆特写图，聚焦模特唇部或眼妆。色彩浓郁饱和，参考大牌彩妆广告。背景虚化处理，突出妆容质感与色彩层次。适合口红、眼影、眼线产品推广。"),
            tpl("fashion-accessory-flatlay", "配饰平铺", "fashion-beauty",
                "俯拍平铺、精致摆拍和高级感。",
                "生成一张配饰平铺图（手表、首饰、墨镜等），俯拍角度。背景为大理石、丝绒或亚麻。配饰摆放讲究构图与节奏，光线柔和。整体高级、克制，适合奢侈品电商详情页。")
        ))

        // real-estate-interior
        addAll(listOf(
            tpl("interior-nordic-living", "北欧客厅", "real-estate-interior",
                "自然木色、柔和采光和舒适感。",
                "生成一张北欧风格客厅效果图，自然木色地板 + 白色墙面。沙发为浅灰布艺，搭配几何图案地毯与绿植。大面积落地窗，柔和自然光。整体清爽舒适，适合年轻家庭户型展示。"),
            tpl("interior-boutique-hotel", "精品酒店套房", "real-estate-interior",
                "高级材质、暖光和度假感。",
                "生成一张精品酒店套房效果图，深色木地板 + 大理石墙面。床品为高级灰或墨绿丝绒，金属灯具点缀。暖黄色氛围灯光，窗外有海景或城市夜景。突出奢华度假感。"),
            tpl("interior-modern-office", "现代办公室", "real-estate-interior",
                "开放办公、协作区和科技公司氛围。",
                "生成一张现代办公室效果图，开放工位 + 协作区 + 绿植墙。原木色桌面 + 黑色金属框架。玻璃隔断、白色墙面、明亮自然光。整体简洁专业，适合科技公司办公空间展示。"),
            tpl("interior-japandi-bedroom", "日式卧室", "real-estate-interior",
                "榻榻米、低饱和和禅意。",
                "生成一张日式风格卧室效果图，榻榻米 + 低矮床架 + 推拉门。低饱和度配色（米色、灰绿、原木）。窗外可见庭院或竹林。整体宁静禅意，适合民宿、酒店展示。"),
            tpl("interior-kitchen-marble", "大理石厨房", "real-estate-interior",
                "大理石台面、不锈钢电器和精致感。",
                "生成一张现代厨房效果图，白色大理石台面 + 不锈钢电器 + 灰色橱柜。中央岛台 + 吧台椅。光线充足，整体干净精致。适合高端公寓厨房展示。")
        ))

        // education-training
        addAll(listOf(
            tpl("edu-course-cover", "课程封面", "education-training",
                "清晰主题、专业感和学习动机。",
                "生成一张在线课程封面图，画面比例 16:9。中央留出课程标题位（实际文字稍后添加），四周用主题图标装饰。配色专业（深蓝、橙色、绿色等），整体传达学习成长感。适合知识付费课程封面。"),
            tpl("edu-knowledge-map", "知识地图", "education-training",
                "适合知识体系、章节路线和学习路径。",
                "生成一张知识地图风格图，中央为主节点，向外辐射子节点（实际文字稍后添加）。整体类似思维导图或路线图，配色清晰、节点层次分明。适合学习路径、章节导览、知识体系介绍。"),
            tpl("edu-kids-cognitive", "儿童认知卡", "education-training",
                "可爱插画、单一知识点和亲和力。",
                "生成一张儿童认知卡插画，主题：{知识点}。卡通风格、色彩明快、造型圆润可爱。背景简洁，主体居中突出。适合 3-6 岁儿童认知启蒙，单卡单知识点。"),
            tpl("edu-textbook-illustration", "教材插图", "education-training",
                "信息图、示意图和教学辅助。",
                "生成一张教材风格的信息插图，主题：{主题}。扁平化矢量风格，配色专业（不刺眼）。标注清晰、层次分明，适合中小学教材或科普读物。")
        ))

        // game-concept
        addAll(listOf(
            tpl("game-character-concept", "角色概念设定", "game-concept",
                "造型、轮廓和性格特征。",
                "生成一张游戏角色概念设定图，角色全身立绘 + 多角度视图。造型要有强识别度，轮廓清晰，色彩搭配体现职业与性格。背景简洁，突出角色。适合 RPG/动作游戏前期角色设计。"),
            tpl("game-fantasy-scene", "奇幻场景", "game-concept",
                "可探索空间、氛围和世界观。",
                "生成一张奇幻游戏场景概念图，参考《艾尔登法环》《巫师》风格。史诗感构图、戏剧光影、丰富细节（建筑、植被、雾气）。氛围神秘或壮丽，适合开放世界探索场景。"),
            tpl("game-sci-fi-level", "科幻关卡", "game-concept",
                "硬表面结构、通道和互动装置。",
                "生成一张科幻游戏关卡概念图，参考《死亡搁浅》《赛博朋克 2077》风格。硬表面建模、金属质感、霓虹灯光、未来科技装置。通道清晰、有探索感，适合 FPS/动作游戏关卡设计。"),
            tpl("game-creature-design", "怪物设计", "game-concept",
                "生物结构、攻击姿态和威胁感。",
                "生成一张游戏怪物概念设计图，怪物全身立绘 + 多角度视图。生物结构合理（解剖学可信），有威胁感与记忆点。配色突出其属性（火、毒、冰等）。适合 RPG/动作游戏敌人设计。"),
            tpl("game-prop-icon", "道具图标", "game-concept",
                "物品图标、清晰辨识和风格统一。",
                "生成一张游戏道具图标，主题：{物品}。扁平或半写实风格，背景透明或纯色。物品居中、辨识度高，适合装备栏/背包图标使用。")
        ))

        // tech-ui
        addAll(listOf(
            tpl("tech-saas-hero", "SaaS 官网首屏", "tech-ui",
                "抽象产品能力、数据和现代科技感。",
                "生成一张 SaaS 官网首屏视觉图，画面比例 16:9。抽象图形（流程图节点、数据流、几何元素）+ 品牌主色渐变背景。整体现代、科技感强，适合 B 端 SaaS 产品官网首屏。"),
            tpl("tech-ai-assistant-mascot", "AI 助手形象", "tech-ui",
                "友好、可信和智能感。",
                "生成一个 AI 助手吉祥物形象设计，参考 Notion AI、Grammarly 等的友好风格。圆润造型、明亮配色（蓝紫为主），表情友好。适合作为 AI 产品官网/启动页/引导页的视觉主角。"),
            tpl("tech-dashboard-mockup", "数据仪表盘", "tech-ui",
                "图表模块、层级和可视化氛围。",
                "生成一张数据仪表盘视觉图，画面比例 16:9。多个图表模块（柱状图、折线图、热力图等）组合布局，深色或浅色主题。整体专业、信息密度高，适合 B 端数据产品宣传图。"),
            tpl("tech-mobile-app-mockup", "移动 App 展示", "tech-ui",
                "手机 Mockup、场景化和现代感。",
                "生成一张移动 App 视觉展示图，中央为 iPhone Mockup（屏幕内容稍后添加），背景为渐变色或场景化（办公桌、咖啡厅等）。整体现代、有故事感，适合 App 推广落地页。")
        ))

        // travel-culture
        addAll(listOf(
            tpl("travel-destination-poster", "目的地海报", "travel-culture",
                "地标、自然景观和旅行向往感。",
                "生成一张旅行目的地海报图，画面比例 3:4。主体为地标建筑或自然景观，光线戏剧化（黄金时刻/蓝调时刻）。顶部或底部留出标题位（实际文字稍后添加）。整体传达旅行向往感。"),
            tpl("travel-city-card", "城市名片", "travel-culture",
                "浓缩城市地标、街景和文化符号。",
                "生成一张城市名片风格图，将多个城市地标/文化符号拼贴于一图（如北京：天坛 + 长城 + 胡同 + 红墙黄瓦）。色彩鲜明，构图紧凑，适合文旅宣传海报。"),
            tpl("travel-resort-aerial", "度假酒店俯瞰", "travel-culture",
                "泳池、海景、房间和松弛感。",
                "生成一张度假酒店俯瞰/鸟瞰图，主体为酒店泳池 + 海景房 + 沙滩椰林。光线为下午侧光，色彩饱和度高。整体传达度假松弛感，适合酒店预订平台宣传。"),
            tpl("travel-local-cuisine", "地道美食地图", "travel-culture",
                "当地特色美食、街头氛围和文化感。",
                "生成一张当地美食地图风格图，画面分为多个区域，每个区域展示一道当地特色美食（实际文字稍后添加）。背景为街头氛围或老城风光。适合文旅美食推广、城市文化宣传。")
        ))

        // health-wellness
        addAll(listOf(
            tpl("wellness-morning-yoga", "晨间瑜伽", "health-wellness",
                "日出、呼吸感和身心平衡。",
                "生成一张晨间瑜伽场景图，模特在户外（草地/海边/山顶）做瑜伽姿势。光线为日出黄金时刻，柔和温暖。整体传达宁静、平衡、身心合一的感觉。适合瑜伽馆/健康 App 宣传。"),
            tpl("wellness-fitness-energy", "健身动感", "health-wellness",
                "力量训练、运动轨迹和积极状态。",
                "生成一张健身场景图，模特做力量训练动作（深蹲/硬拉/卧推）。光线为戏剧化侧光，强调肌肉线条与力量感。背景为现代健身房，整体积极向上、充满动感。"),
            tpl("wellness-meditation-space", "冥想疗愈空间", "health-wellness",
                "柔光、香薰、坐垫和宁静氛围。",
                "生成一张冥想疗愈空间图，主体为坐垫 + 香薰 + 蜡烛 + 绿植。光线为暖黄柔光，色彩低饱和。整体宁静、疗愈感强，适合冥想 App/瑜伽馆/SPA 馆宣传。"),
            tpl("wellness-healthy-meal", "健康餐", "health-wellness",
                "营养均衡、清新摆盘和健康生活。",
                "生成一张健康餐摆盘图，俯拍角度。盘中有均衡的蛋白质、碳水、蔬菜（鸡胸肉、藜麦、西兰花等）。背景为木质餐桌 + 餐具 + 柠檬水。整体清新、健康、有食欲感。")
        ))

        // portrait-avatar
        addAll(listOf(
            tpl("avatar-business-headshot", "职业头像", "portrait-avatar",
                "干净背景、可信气质和商务用途。",
                "生成一张职业商务头像，画面比例 1:1。模特半身像，穿西装/职业装，表情自信专业。背景为纯色或渐变（深蓝、灰、米色）。光线柔和均匀，适合 LinkedIn/企业官网/简历使用。"),
            tpl("avatar-social-friendly", "社交头像", "portrait-avatar",
                "鲜明个性、友好表情和识别度。",
                "生成一张社交媒体头像，画面比例 1:1。模特表情友好自然，色彩明快（参考 Instagram 滤镜风格）。背景简洁或虚化处理，突出人物个性。适合 Instagram/微信/小红书头像。"),
            tpl("avatar-character-fantasy", "角色头像", "portrait-avatar",
                "适合虚拟形象、播客和社区账号。",
                "生成一张角色风格头像，画面比例 1:1。可以是奇幻/科幻/动漫风格的角色立绘。色彩鲜明，造型有记忆点，适合虚拟主播/播客/社区账号头像使用。"),
            tpl("avatar-cute-illustration", "萌系插画头像", "portrait-avatar",
                "可爱卡通、圆润造型和亲和力。",
                "生成一张萌系插画风格头像，画面比例 1:1。圆润造型、大眼睛、明亮配色。可以是动物、Q 版人物或拟人化角色。适合个人博客/社交账号/儿童品牌头像。")
        ))

        // business-office
        addAll(listOf(
            tpl("biz-report-cover", "商业报告封面", "business-office",
                "适合年报、行业研究和咨询报告。",
                "生成一张商业报告封面图，画面比例 A4 纵向。中央留出报告标题位（实际文字稍后添加），四周用抽象图形装饰（流程图节点、数据流、几何元素）。整体专业、克制、有质感，适合咨询/研究机构报告。"),
            tpl("biz-strategy-diagram", "咨询战略图", "business-office",
                "高层汇报、战略方向和结构化表达。",
                "生成一张咨询战略风格图，画面比例 16:9。包含战略框架图（实际文字稍后添加）+ 抽象图形装饰。配色专业（深蓝、灰、橙），整体结构化、有逻辑感，适合咨询提案/战略汇报。"),
            tpl("biz-team-collaboration", "团队协作", "business-office",
                "会议、白板和跨部门合作。",
                "生成一张团队协作场景图，现代化办公室内，3-5 人在白板前讨论。光线明亮自然，氛围积极专业。适合企业官网 About/招聘页面/内部宣传使用。"),
            tpl("biz-pitch-deck-cover", "Pitch Deck 封面", "business-office",
                "创业融资、专业感和现代设计。",
                "生成一张创业融资 Pitch Deck 封面图，画面比例 16:9。中央留出公司名/Logo 位（实际文字稍后添加），背景为现代渐变 + 抽象图形。整体专业、有设计感，适合种子轮/A 轮融资演示文稿封面。")
        ))

        // seasonal-festival
        addAll(listOf(
            tpl("seasonal-spring-new", "春日上新", "seasonal-festival",
                "花朵、浅色和轻盈生机。",
                "生成一张春日上新视觉图，画面比例 1:1。樱花/桃花/郁金香等春花元素 + 浅色背景 + 商品居中。色调粉嫩清新（粉、浅绿、米白）。整体传达春日上新、轻盈生机感，适合春季营销活动。"),
            tpl("seasonal-summer-cool", "夏日清凉", "seasonal-festival",
                "冰块、水波、阳光和明亮色彩。",
                "生成一张夏日清凉视觉图，画面比例 1:1。冰块、水波纹、阳光透射等元素 + 商品居中。色调偏冷蓝亮黄对比。整体传达清凉解暑感，适合夏季饮料/冰淇淋/防晒产品营销。"),
            tpl("seasonal-autumn-warm", "秋日暖调", "seasonal-festival",
                "枫叶、木质、金色阳光和收获感。",
                "生成一张秋日暖调视觉图，画面比例 1:1。枫叶、木质背景、金色阳光等元素 + 商品居中。色调偏暖（橙、棕、金）。整体传达秋日温暖、收获感，适合秋季营销活动。"),
            tpl("seasonal-winter-snow", "冬日雪景", "seasonal-festival",
                "雪花、温暖灯光和节日氛围。",
                "生成一张冬日雪景视觉图，画面比例 1:1。雪花飘落、温暖灯光、圣诞树等元素 + 商品居中。色调偏冷暖对比（白蓝雪景 + 暖黄灯光）。整体传达冬日节日氛围，适合圣诞/新年营销。"),
            tpl("seasonal-cny-red", "春节红", "seasonal-festival",
                "中国红、金箔、福字和喜庆氛围。",
                "生成一张春节视觉图，画面比例 1:1。中国红背景 + 金箔/福字/灯笼等元素 + 商品居中。色调红金为主，整体喜庆、有年味，适合春节营销活动。")
        ))

        // texture-background
        addAll(listOf(
            tpl("texture-gradient-mesh", "网格渐变背景", "texture-background",
                "柔和多色渐变，适合科技和海报背景。",
                "生成一张网格渐变背景图，画面比例 16:9。多色渐变（如蓝紫粉、绿黄橙、青蓝紫），柔和过渡，无主体。适合作为海报/PPT/官网首屏背景使用。"),
            tpl("texture-marble", "大理石纹理", "texture-background",
                "石材纹理、高级留白和包装感。",
                "生成一张大理石纹理背景图，画面比例 1:1。白色或深色大理石，纹理自然流畅，有金色或灰色脉络。整体高级、克制，适合奢侈品/化妆品包装/海报背景。"),
            tpl("texture-paper-grain", "纸张颗粒", "texture-background",
                "轻微纤维、印刷质感和温和底纹。",
                "生成一张纸张颗粒纹理背景图，画面比例 1:1。米色或白色纸张，轻微纤维感、温和底纹。适合作为书籍封面/包装/海报背景，传达手工与温度感。"),
            tpl("texture-wood-grain", "木纹", "texture-background",
                "原木纹理、温暖色调和自然感。",
                "生成一张木纹纹理背景图，画面比例 1:1。原木横截面或长纹，色调为温暖原木色（浅橡、深胡桃等）。适合作为家居/餐饮/手作品牌背景使用。"),
            tpl("texture-watercolor-wash", "水彩晕染", "texture-background",
                "色彩晕染、艺术感和柔和过渡。",
                "生成一张水彩晕染纹理背景图，画面比例 1:1。多色水彩自然晕染、边缘柔和过渡。色彩可选（粉紫、青绿、橙黄等）。适合艺术海报/婚礼请柬/儿童品牌背景。")
        ))
    }

    // ============ 辅助构造函数 ============
    private fun cat(categoryId: String, name: String, description: String, sortOrder: Long): PromptTemplateCategory =
        PromptTemplateCategory(
            categoryId = categoryId,
            name = name,
            description = description,
            source = PromptTemplateCategory.SOURCE_DEFAULT,
            sortOrder = sortOrder
        )

    private fun tpl(
        templateId: String,
        name: String,
        categoryId: String,
        description: String,
        prompt: String
    ): PromptTemplate = PromptTemplate(
        templateId = templateId,
        name = name,
        categoryId = categoryId,
        prompt = prompt,
        description = description,
        source = PromptTemplate.SOURCE_DEFAULT,
        createdAt = 0L,
        updatedAt = 0L
    )
}
