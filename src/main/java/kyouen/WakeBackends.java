package kyouen;

import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import net.sf.jsr107cache.Cache;

import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.EntityNotFoundException;
import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.KeyFactory;
import com.google.appengine.api.datastore.Text;
import com.google.appengine.api.taskqueue.Queue;
import com.google.appengine.api.taskqueue.QueueFactory;
import com.google.appengine.api.taskqueue.TaskOptions.Builder;
import com.google.appengine.api.taskqueue.TaskOptions.Method;

@SuppressWarnings("serial")
public class WakeBackends extends HttpServlet {
	static String namelist = 
			",青野武,,-1,あおのたけし,青山穣,,-1,あおやまゆたか,赤羽根健治,,-1,あかばねけんじ,秋元羊介,,-1,あきもとようすけ,浅沼晋太郎,,-1,あさぬましんたろう,浅野要二,,-1,あさのようじ,浅利遼太,,-1,あさりりょうた,阿部敦,,-1,あべあつし,天田益男,,-1,あまだますお,有本欽隆,,-1,ありもときんりゅう" +
			",飯田利信,,-1,いいだとしのぶ,飯田浩志,,-1,いいだひろし,飯塚昭三,&i=1,-1,いいづかしょうぞう,井口祐一,,-1,いぐちゆういち,池田秀一,&i=1,-1,いけだしゅういち,池田勝,,-1,いけだまさる,池水通洋,,-1,いけみずみちひろ,石井康嗣,,-1,いしいこうじ,石井真,,-1,いしいまこと,石川英郎,,-1,いしかわひでお,石田彰,,-1,いしだあきら,石田太郎,,-1,いしだたろう,石塚運昇,,-1,いしづかうんしょう,石塚堅,,-1,いしづかかたし,石野竜三,,-1,いしのりゅうぞう,石丸博也,,-1,いしまるひろや,石森達幸,,-1,いしもりたっこう,磯部弘,,-1,いそべひろし,市川治,,-1,いちかわおさむ,市来光弘,,-1,いちきみつひろ,一条和矢,,-1,いちじょうかずや,市瀬秀和,,-1,いちのせひでかず,伊藤健太郎,_(声優),-1,いとうけんたろう,伊東隼人,,-1,いとうはやと,稲田徹,,-1,いなだてつ,稲葉実,,-1,いなばみのる,井上和彦,_(声優),-1,いのうえかずひこ,井上剛,_(声優),-1,いのうえごう,井上優,_(声優),-1,いのうえすぐる,井上真樹夫,,-1,いのうえまきお,伊丸岡篤,,-1,いまるおかあつし,入野自由,&i=1,-1,いりのみゆ,岩崎征実,,-1,いわさきまさみ,岩田光央,,-1,いわたみつお,岩永哲哉,,-1,いわながてつや" +
			",上田敏也,,-1,うえだとしや,うえだゆうじ,,-1,うえだゆうじ,上田燿司,,-1,うえだようじ,宇垣秀成,,-1,うがきひでなり,内田岳志,,-1,うちださとし,内田直哉,,-1,うちだなおや,内山昂輝,,-1,うちやまこうき,内海賢二,,-1,うつみけんじ,梅津秀行,,-1,うめづひでゆき,浦田優,,-1,うらたゆう,江川大輔,,-1,えがわだいすけ,江川央生,,-1,えがわひさお,江口拓也,,-1,えぐちたくや,江原正士,,-1,えばらまさし" +
			",大川透,,-1,おおかわとおる,大須賀純,,-1,おおすかじゅん,大竹宏,,-1,おおたけひろし,太田真一郎,,-1,おおたしんいちろう,太田哲治,,-1,おおたてつはる,大塚明夫,,-1,おおつかあきお,大塚周夫,,-1,おおつかちかお,大塚芳忠,,-1,おおつかほうちゅう,大友龍三郎,,-1,おおともりゅうざぶろう,大西健晴,,-1,おおにしたけはる,大場真人,,-1,おおばまひと,大原崇,,-1,おおはらたかし,緒方賢一,,-1,おがたけんいち,岡野浩介,,-1,おかのこうすけ,岡本信彦,,-1,おかもとのぶひこ,小川真司,,-1,おがわしんじ,置鮎龍太郎,,-1,おきあゆりょうたろう,興津和幸,,-1,おきつかずゆき,小栗旬,,-1,おぐりしゅん,斧アツシ,,-1,おのあつし,小野賢章,,-1,おのけんしょう,小野坂昌也,,-1,おのさかまさや,小野大輔,,-1,おのだいすけ,小野友樹,,-1,おのゆうき" +
			",柿原徹也,,-1,かきはらてつや,掛川裕彦,,-1,かけがわひろひこ,風間勇刀,,-1,かざまゆうと,樫井笙人,,-1,かしいしょうと,梶裕貴,,-1,かじゆうき,加瀬康之,,-1,かせやすゆき,勝杏里,,-1,かつあんり,加藤精三,_(声優),-1,かとうせいぞう,金尾哲夫,,-1,かなおてつお,蟹江栄司,,-1,かにええいじ,金丸淳一,,-1,かねまるじゅんいち,金光宣明,,-1,かねみつのぶあき,神谷明,,-1,かみやあきら,神谷浩史,,-1,かみやひろし,家弓家正,,-1,かゆみいえまさ,川島得愛,,-1,かわしまとくよし,川田紳司,,-1,かわだしんじ,川津泰彦,,-1,かわづやすひこ,かわのをとや,,-1,かわのおとや,川原慶久,,-1,かわはらよしひさ,河本邦弘,,-1,かわもとくにひろ,神奈延年,,-1,かんなのぶとし" +
			",キートン山田,,-1,きいとんやまだ,木内秀信,,-1,きうちひでのぶ,菊池正美,,-1,きくちまさみ,私市淳,,-1,きさいちあつし,岸尾だいすけ,,-1,きしおだいすけ,北村弘一,,-1,きたむらこういち,木村幌,,-1,きむらあきら,木村昴,,-1,きむらすばる,木村良平,,-1,きむらりょうへい,肝付兼太,,-1,きもつきかねた,喜安浩平,,-1,きやすこうへい,清川元夢,,-1,きよかわもとむ,銀河万丈,,-1,ぎんがばんじょう" +
			",草尾毅,,-1,くさおたけし,楠大典,,-1,くすのきたいてん,黒田崇矢,,-1,くろだたかや,KENN,&i=1,-1,けん,玄田哲章,,-1,げんだてっしょう,郷田ほづみ,,-1,ごうだほづみ,郷里大輔,,-1,ごうりだいすけ,越田直樹,,-1,こしだなおき,小杉十郎太,,-1,こすぎじゅうろうた,小西克幸,,-1,こにしかつゆき,小林清志,,-1,こばやしきよし,こぶしのぶゆき,,-1,こぶしのぶゆき,小宮山清,,-1,こみやまきよし,子安武人,,-1,こやすたけひと,小山剛志,,-1,こやまつよし,小山力也,,-1,こやまりきや,近藤隆,,-1,こんどうたかし,近藤孝行,,-1,こんどうたかゆき,金野潤,_(声優),-1,こんのじゅん" +
			",斎藤志郎,,-1,さいとうしろう,酒井哲也,,-1,さかいてつや,阪脩,,-1,さかおさむ,坂口候一,,-1,さかぐちこういち,阪口周平,,-1,さかぐちしゅうへい,阪口大助,,-1,さかぐちだいすけ,櫻井孝宏,,-1,さくらいたかひろ,櫻井トオル,,-1,さくらいとおる,桜井敏治,,-1,さくらいとしはる,笹岡繁蔵,,-1,ささおかしげぞう,ささきいさお,,-1,ささきいさお,佐々木望,,-1,ささきのぞむ,笹沼尭羅,,-1,ささぬまあきら,佐藤健輔,,-1,さとうけんすけ,佐藤拓也,_(声優),-1,さとうたくや,里内信夫,,-1,さとうちしのぶ,佐藤正治,_(声優),-1,さとうまさはる,沢りつお,,-1,さわりつお,塩沢兼人,,-1,しおざわかねと,塩屋浩三,,-1,しおやこうぞう,塩屋翼,,-1,しおやよく,篠原まさのり,,-1,しのはらまさのり,四宮豪,,-1,しのみやごう,柴田秀勝,,-1,しばたひでかつ,渋谷茂,,-1,しぶやしげる,島香裕,,-1,しまかゆう,島崎信長,,-1,しまざきのぶなが,島田敏,,-1,しまだびん,嶋田真,,-1,しまだまこと,志村知幸,,-1,しむらともゆき,下崎紘史,,-1,しもざきひろし,下野紘,,-1,しものひろ,下山吉光,,-1,しもやまよしみつ,下和田ヒロキ,,-1,しもわだひろき,白石稔,,-1,しらいしみのる,白鳥哲,&i=1,-1,しらとりてつ" +
			",菅沼久義,,-1,すがぬまひさよし,杉田智和,,-1,すぎたともかず,杉山紀彰,,-1,すぎやまのりあき,菅生隆之,,-1,すごうたかゆき,鈴置洋孝,,-1,すずおきひろたか,鈴木勝美,,-1,すずきかつみ,鈴木達央,,-1,すずきたつひさ,鈴木千尋,_(声優),-1,すずきちひろ,鈴村健一,,-1,すずむらけんいち,陶山章央,,-1,すやまあきお,諏訪部順一,,-1,すわべじゅんいち,関俊彦,,-1,せきとしひこ,関智一,,-1,せきともかず,園部啓一,,-1,そのべけいいち" +
			",泰勇気,,-1,たいゆうき,高木俊,,-1,たかぎしゅん,高木均,,-1,たかぎひとし,高城元気,,-1,たかぎもとき,高木渉,,-1,たかぎわたる,高階俊嗣,,-1,たかしなとしつぐ,高田裕司,_(声優),-1,たかだゆうじ,高塚正也,,-1,たかつかまさや,高戸靖広,,-1,たかとやすひろ,高橋伸也,,-1,たかはししんや,高橋広樹,,-1,たかはしひろき,滝口順平,,-1,たきぐちじゅんぺい,武内健,,-1,たけうちけん,武虎,,-1,たけとら,竹村拓,,-1,たけむらひろし,竹本英史,,-1,たけもとえいじ,竹若拓磨,,-1,たけわかたくま,多田野曜平,,-1,ただのようへい,立木文彦,,-1,たちきふみひこ,立花慎之介,,-1,たちばなしんのすけ,龍田直樹,,-1,たつたなおき,たてかべ和也,,-1,たてかべかずや,田中一成,,-1,たなかかずなり,田中秀幸,_(声優),-1,たなかひでゆき,田中亮一,,-1,たなかりょういち,谷口節,,-1,たにぐちたかし,谷山紀章,,-1,たにやまきしょう,田の中勇,,-1,たのなかいさむ,田丸篤志,,-1,たまるあつし" +
			",千葉一伸,,-1,ちばいっしん,千葉繁,,-1,ちばしげる,千葉進歩,,-1,ちばすすむ,茶風林,,-1,ちゃふうりん,チョー,_(俳優),-1,ちょー,塚田正昭,,-1,つかだまさあき,津嘉山正種,,-1,つかやままさね,津久井教生,,-1,つくいきょうせい,辻親八,,-1,つじしんぱち,辻谷耕史,,-1,つじたにこうじ,津田英三,,-1,つだえいぞう,津田健次郎,&i=1,-1,つだけんじろう,土田大,,-1,つちだひろし,坪井智浩,,-1,つぼいともひろ,鶴岡聡,,-1,つるおかさとし,寺島拓篤,,-1,てらしまたくま,てらそままさき,,-1,てらそままさき,東地宏樹,,-1,とうちひろき,遠近孝一,,-1,とおちかこういち,戸北宗寛,,-1,ときたむねひろ,徳本英一郎,,-1,とくもとえいいちろう,戸谷公次,,-1,とたにこうじ,利根健太朗,,-1,とねけんたろう,飛田展男,,-1,とびたのぶお,戸部公爾,,-1,とべこうじ,富田耕生,,-1,とみたこうせい,富山敬,,-1,とみやまけい,土門仁,,-1,どもんじん,豊永利行,&i=1,-1,とよながとしゆき,鳥海浩輔,,-1,とりうみこうすけ" +
			",内藤玲,,-1,ないとうりょう,永井一郎,,-1,ながいいちろう,中井和哉,,-1,なかいかずや,中尾隆聖,,-1,なかおりゅうせい,長嶝高士,,-1,ながさこたかし,中嶋聡彦,,-1,なかじまとしひこ,中田和宏,,-1,なかたかずひろ,中田譲治,&i=1,-1,なかたじょうじ,中原茂,,-1,なかはらしげる,中博史,,-1,なかひろし,中村大樹,,-1,なかむらだいき,中村悠一,,-1,なかむらゆういち,浪川大輔,,-1,なみかわだいすけ,納谷悟朗,,-1,なやごろう,納谷六朗,,-1,なやろくろう,奈良徹,,-1,ならとおる,成田剣,,-1,なりたけん,成瀬誠,,-1,なるせまこと,難波圭一,,-1,なんばけいいち,西村朋紘,,-1,にしむらともひろ,西村知道,,-1,にしむらともみち,西凜太朗,,-1,にしりんたろう,西脇保,,-1,にしわきたもつ,沼田祐介,,-1,ぬまたゆうすけ,根本正勝,,-1,ねもとまさかず,野沢那智,,-1,のざわなち,野島昭生,,-1,のじまあきお,野島健児,_(声優),-1,のじまけんじ,野島裕史,,-1,のじまゆうじ,野田圭一,,-1,のだけいいち,野宮一範,,-1,のみやかずのり,乃村健次,,-1,のむらけんじ" +
			",萩道彦,,-1,はぎみちひこ,萩原聖人,,-1,はぎわらまさと,羽佐間道夫,,-1,はざまみちお,土師孝也,,-1,はしたかや,橋本晃一,,-1,はしもとこういち,長谷部浩一,,-1,はせべこういち,畠中祐,,-1,はたなかたすく,羽多野渉,,-1,はたのわたる,浜田賢二,,-1,はまだけんじ,速水奨,,-1,はやみしょう,日野聡,,-1,ひのさとし,上別府仁資,,-1,びふひとし,檜山修之,,-1,ひやまのぶゆき,平井達矢,,-1,ひらいたつや,平川大輔,,-1,ひらかわだいすけ,平田広明,&i=1,-1,ひらたひろあき,平野正人,,-1,ひらのまさと,広川太一郎,,-1,ひろかわたいちろう,広瀬正志,,-1,ひろせまさし,廣田行生,,-1,ひろたこうせい,福島潤,,-1,ふくしまじゅん,福山潤,,-1,ふくやまじゅん,藤田大助,_(声優),-1,ふじただいすけ,藤本たかひろ,,-1,ふじもとたかひろ,藤本譲,,-1,ふじもとゆずる,藤原啓治,,-1,ふじわらけいじ,藤原祐規,,-1,ふじわらゆうき,二又一成,,-1,ふたまたいっせい,古川登志夫,,-1,ふるかわとしお,古澤徹,,-1,ふるさわとおる,古島清孝,,-1,ふるしまきよたか,古谷徹,,-1,ふるやとおる" +
			",保志総一朗,,-1,ほしそういちろう,星野貴紀,,-1,ほしのたかのり,星野充昭,,-1,ほしのみつあき,細井治,,-1,ほそいおさむ,細谷佳正,,-1,ほそやよしまさ,堀内賢雄,,-1,ほりうちけんゆう,堀江一眞,,-1,ほりえかずま,堀川りょう,,-1,ほりかわりょう,堀秀行,,-1,ほりひでゆき,堀之紀,,-1,ほりゆきとし,本城雄太郎,,-1,ほんじょうゆうたろう,マイケル・リーバス,,-1,まいけるりーばす,前野智昭,,-1,まえのともあき,間島淳司,,-1,まじまじゅんじ,増岡弘,,-1,ますおかひろし,増田俊樹,,-1,ますだとしき,増谷康紀,,-1,ますたにやすのり,増田裕生,,-1,ますだゆうき,松岡大介,,-1,まつおかだいすけ,松岡禎丞,,-1,まつおかよしつぐ,松尾銀三,,-1,まつおぎんぞう,松風雅也,,-1,まつかぜまさや,松田健一郎,,-1,まつだけんいちろう,松田佑貴,,-1,まつだゆうき,松野太紀,,-1,まつのたいき,松本保典,,-1,まつもとやすのり,松山鷹志,,-1,まつやまたかし,真殿光昭,,-1,まどのみつあき,丸山詠二,&i=1,-1,まるやまえいじ" +
			",三浦祥朗,,-1,みうらひろあき,三木眞一郎,,-1,みきしんいちろう,水島大宙,,-1,みずしまたかひろ,水島裕,,-1,みずしまゆう,水鳥鐵夫,,-1,みずとりてつお,三ツ矢雄二,,-1,みつやゆうじ,緑川光,,-1,みどりかわひかる,三宅健太,,-1,みやけけんた,三宅淳一,,-1,みやけじゅんいち,宮崎一成,,-1,みやざきいっせい,宮下栄治,,-1,みやしたえいじ,宮田幸季,,-1,みやたこうき,宮野真守,&i=1,-1,みやのまもる,宮本充,,-1,みやもとみつる,麦人,,-1,むぎひと,室園丈裕,,-1,むろぞのたけひろ,最上嗣生,,-1,もがみつぐお,望月健一,,-1,もちづきけんいち,森功至,,-1,もりかつじ,森川智之,,-1,もりかわとしゆき,森久保祥太郎,,-1,もりくぼしょうたろう,森田順平,,-1,もりたじゅんぺい,森田成一,,-1,もりたまさかず" +
			",矢尾一樹,,-1,やおかずき,矢島正明,,-1,やじままさあき,八代駿,,-1,やしろしゅん,安井邦彦,,-1,やすいくにひこ,安原義人,,-1,やすはらよしと,保村真,,-1,やすむらまこと,安元洋貴,,-1,やすもとひろき,矢田耕司,,-1,やだこうじ,家中宏,,-1,やなかひろし,梁田清之,,-1,やなだきよゆき,八奈見乗児,,-1,やなみじょうじ,矢部雅史,,-1,やべまさひと,山口勝平,,-1,やまぐちかっぺい,山口清裕,,-1,やまぐちきよひろ,山口健,,-1,やまぐちけん,山崎たくみ,,-1,やまざきたくみ,山路和弘,,-1,やまじかずひろ,山下啓介,,-1,やましたけいすけ,山田真一,_(声優),-1,やまだしんいち,山寺宏一,,-1,やまでらこういち,山野井仁,,-1,やまのいじん,山本和臣,,-1,やまもとかずとみ,屋良有作,,-1,やらゆうさく,優希比呂,,-1,ゆうきひろ,遊佐浩二,,-1,ゆさこうじ,吉野裕行,,-1,よしのひろゆき,代永翼,,-1,よながつばさ,若本規夫,,-1,わかもとのりお,渡部猛,,-1,わたべたけし" +
			",愛河里花子,,1,あいかわりかこ,相沢恵子,,1,あいざわけいこ,相沢舞,,1,あいざわまい,青山ゆかり,,1,あおやまゆかり,赤崎千夏,,1,あかざきちなつ,明坂聡美,,1,あけさかさとみ,麻上洋子,,1,あさがみようこ,浅川悠,,1,あさかわゆう,浅倉杏美,,1,あさくらあずみ,浅野真澄,,1,あさのますみ,阿澄佳奈,,1,あすみかな,安達忍,,1,あだちしのぶ,安達まり,,1,あだちまり,天野由梨,,1,あまのゆり,新井里美,,1,あらいさとみ,荒木香衣,,1,あらきかえ,荒浪和沙,,1,あらなみかずさ,安藤麻吹,,1,あんどうまぶき" +
			",飯塚雅弓,,1,いいづかまゆみ,五十嵐裕美,,1,いがらしひろみ,井口裕香,,1,いぐちゆか,池澤春菜,,1,いけざわはるな,池田昌子,,1,いけだまさこ,石川綾乃,,1,いしかわあやの,石川静,_(声優),1,いしかわしずか,石原夏織,,1,いしはらかおり,伊瀬茉莉也,,1,いせまりや,壱智村小真,,1,いちむらおま,一龍斎貞弥,,1,いちりゅうさいていや,一龍斎貞友,,1,いちりゅうさいていゆう,伊藤亜矢子,,1,いとうあやこ,伊藤かな恵,,1,いとうかなえ,伊藤静,,1,いとうしずか,伊藤美紀,_(声優),1,いとうみき,稲村優奈,,1,いなむらゆうな,犬山イヌコ,,1,いぬやまいぬこ,井上喜久子,,1,いのうえきくこ,井上奈々子,,1,いのうえななこ,井上麻里奈,,1,いのうえまりな,井上瑤,,1,いのうえよう,いのくちゆか,,1,いのくちゆか,今井麻美,,1,いまいあさみ,岩男潤子,,1,いわおじゅんこ" +
			",上坂すみれ,,1,うえさかすみれ,植田佳奈,,1,うえだかな,うえだ星子,,1,うえだせいこ,上村典子,,1,うえむらのりこ,薄井千織,,1,うすいちおり,内田彩,,1,うちだあや,内田真礼,,1,うちだまあや,浦和めぐみ,,1,うらわめぐみ,榎本温子,,1,えのもとあつこ,江森浩子,,1,えもりひろこ,遠藤綾,,1,えんどうあや" +
			",及川ひとみ,,1,おいかわひとみ,大浦冬華,,1,おおうらふゆか,大亀あすか,,1,おおがめあすか,大久保瑠美,,1,おおくぼるみ,大谷育江,,1,おおたにいくえ,大坪由佳,,1,おおつぼゆか,大橋歩夕,,1,おおはしあゆる,大原さやか,,1,おおはらさやか,大原めぐみ,,1,おおはらめぐみ,大本眞基子,,1,おおもとまきこ,大山のぶ代,,1,おおやまのぶよ,小笠原亜里沙,,1,おがさわらありさ,岡嶋妙,,1,おかじまたえ,緒方恵美,,1,おがたえみ,岡村明美,,1,おかむらあけみ,岡本麻弥,,1,おかもとまや,沖佳苗,&i=1,1,おきかなえ,小倉唯,,1,おぐらゆい,尾崎恵,,1,おざきめぐみ,織田芙実,,1,おだふみ,小野涼子,,1,おのりょうこ,小幡真裕,,1,おばたまゆ,小原乃梨子,,1,おはらのりこ,小見川千明,,1,おみがわちあき,おみむらまゆこ,,1,おみむらまゆこ,折笠愛,,1,おりかさあい,折笠富美子,,1,おりかさふみこ" +
			",甲斐田裕子,,1,かいだゆうこ,甲斐田ゆき,,1,かいだゆき,かかずゆみ,,1,かかずゆみ,風音,_(声優),1,かざね,笠原弘子,,1,かさはらひろこ,笠原留美,,1,かさはらるみ,片岡あづさ,,1,かたおかあずさ,勝生真沙子,,1,かつきまさこ,加藤英美里,,1,かとうえみり,加藤奈々絵,,1,かとうななえ,加藤みどり,,1,かとうみどり,加藤優子,,1,かとうゆうこ,門脇舞以,,1,かどわきまい,かないみか,,1,かないみか,金田アキ,,1,かなだあき,金田朋子,,1,かねだともこ,金元寿子,,1,かねもとひさこ,鹿野優以,,1,かのゆい,茅野愛衣,,1,かやのあい,川上とも子,,1,かわかみともこ,川島千代子,,1,かわしまちよこ,川澄綾子,,1,かわすみあやこ,川田妙子,,1,かわだたえこ,川浪葉子,,1,かわなみようこ,川村万梨阿,,1,かわむらまりあ,河原木志穂,,1,かわらぎしほ,神田朱未,,1,かんだあけみ" +
			",木内レイコ,,1,きうちれいこ,木川絵理子,,1,きがわえりこ,菊池こころ,,1,きくちこころ,菊地祥子,,1,きくちしょうこ,菊地美香,,1,きくちみか,喜多村英梨,,1,きたむらえり,橘田いずみ,,1,きったいずみ,木下紗華,,1,きのしたさやか,儀武ゆう子,,1,ぎぶゆうこ,木村亜希子,,1,きむらあきこ,樹元オリエ,,1,きもとおりえ,京田尚子,,1,きょうだひさこ,金月真美,,1,きんげつまみ,釘宮理恵,,1,くぎみやりえ,くじら,_(声優),1,くじら,くまいもとこ,,1,くまいもとこ,倉田雅世,,1,くらたまさよ,栗林みな実,,1,くりばやしみなみ,桑島法子,,1,くわしまほうこ,桑谷夏子,,1,くわたになつこ" +
			",合田彩,,1,ごうだあや,幸田夏穂,,1,こうだかほ,國府田マリ子,,1,こうだまりこ,こおろぎさとみ,,1,こおろぎさとみ,五行なずな,,1,ごぎょうなずな,小暮英麻,,1,こぐれえま,小桜エツコ,,1,こざくらえつこ,小島幸子,,1,こじまさちこ,小清水亜美,,1,こしみずあみ,小平有希,,1,こだいらゆうき,後藤沙緒里,,1,ごとうさおり,後藤麻衣,_(声優),1,ごとうまい,後藤邑子,,1,ごとうゆうこ,寿美菜子,,1,ことぶきみなこ,小林沙苗,,1,こばやしさなえ,小林美佐,,1,こばやしみさ,小林ゆう,,1,こばやしゆう,小林優子,,1,こばやしゆうこ,小林由美子,,1,こばやしゆみこ,小松未可子,,1,こまつみかこ,小松由佳,_(声優),1,こまつゆか,小松里歌,,1,こまつりか,こやまきみこ,,1,こやまきみこ,小山茉美,,1,こやままみ,近藤佳奈子,,1,こんどうかなこ,今野宏美,,1,こんのひろみ" +
			",斎賀みつき,,1,さいがみつき,齋藤彩夏,,1,さいとうあやか,斉藤貴美子,,1,さいとうきみこ,斎藤千和,,1,さいとうちわ,斎藤桃子,,1,さいとうももこ,斉藤佑圭,,1,さいとうゆか,酒井香奈子,,1,さかいかなこ,榊原ゆい,,1,さかきばらゆい,榊原良子,,1,さかきばらよしこ,阪田佳代,,1,さかたかよ,坂本千夏,,1,さかもとちか,坂本真綾,,1,さかもとまあや,佐久間紅美,,1,さくまくみ,佐久間レイ,,1,さくまれい,佐倉綾音,,1,さくらあやね,櫻井智,,1,さくらいとも,櫻井浩美,,1,さくらいはるみ,佐々木日菜子,,1,ささきひなこ,ささきのぞみ,,1,ささきのぞみ,佐々木未来,,1,ささきみこい,佐々木優子,,1,ささきゆうこ,笹本優子,,1,ささもとゆうこ,佐藤朱,,1,さとうあけみ,佐藤聡美,,1,さとうさとみ,佐藤利奈,,1,さとうりな,佐土原かおり,,1,さどはらかおり,真田アサミ,,1,さなだあさみ,紗ゆり,,1,さゆり,沢城みゆき,,1,さわしろみゆき,三瓶由布子,,1,さんぺいゆうこ" +
			",椎名へきる,,1,しいなへきる,宍戸留美,,1,ししどるみ,下屋則子,,1,したやのりこ,篠原恵美,,1,しのはらえみ,芝原のぞみ,,1,しばはらのぞみ,島津冴子,,1,しまづさえこ,島本須美,,1,しまもとすみ,清水愛,,1,しみずあい,清水香里,,1,しみずかおり,志村由美,,1,しむらゆみ,下田麻美,,1,しもだあさみ,庄司宇芽香,,1,しょうじうめか,城雅子,,1,じょうまさこ,荘真由美,,1,しょうまゆみ,白石冬美,,1,しらいしふゆみ,白石涼子,,1,しらいしりょうこ,白鳥由里,,1,しらとりゆり,新谷良子,,1,しんたにりょうこ,真堂圭,,1,しんどうけい,進藤尚美,,1,しんどうなおみ" +
			",杉本沙織,,1,すぎもとさおり,杉本ゆう,,1,すぎもとゆう,杉山佳寿子,,1,すぎやまかずこ,鈴木晶子,,1,すずきあきこ,鈴木真仁,,1,すずきまさみ,鈴木麻里子,,1,すずきまりこ,住友優子,,1,すみともゆうこ,瀬戸麻沙美,,1,せとあさみ,世戸さおり,,1,せとさおり,園崎未恵,,1,そのざきみえ" +
			",高垣彩陽,,1,たかがきあやひ,高木礼子,,1,たかぎれいこ,高口幸子,,1,たかぐちゆきこ,高島雅羅,,1,たかしまがら,高田由美,,1,たかだゆみ,高乃麗,,1,たかのうらら,高梁碧,,1,たかはしあお,高橋和枝,,1,たかはしかずえ,たかはし智秋,,1,たかはしちあき,高橋美佳子,,1,たかはしみかこ,高部あい,,1,たかべあい,高本めぐみ,,1,たかもとめぐみ,鷹森淑乃,,1,たかもりとしの,高森奈津美,,1,たかもりなつみ,高山みなみ,,1,たかやまみなみ,滝沢久美子,,1,たきざわくみこ,瀧本富士子,,1,たきもとふじこ,田口宏子,,1,たぐちひろこ,竹内順子,,1,たけうちじゅんこ,竹達彩奈,,1,たけたつあやな,立野香菜子,,1,たてのかなこ,田中敦子,_(声優),1,たなかあつこ,田中真弓,,1,たなかまゆみ,田中理恵,_(声優),1,たなかりえ,谷井あすか,,1,たにいあすか,田野めぐみ,,1,たのめぐみ,玉川砂記子,,1,たまがわさきこ,田村睦心,,1,たむらむつみ,田村ゆかり,,1,たむらゆかり,TARAKO,,1,たらこ,タルタエリ,,1,たるたえり,丹下桜,,1,たんげさくら" +
			",千々松幸子,,1,ちぢまつさちこ,千葉紗子,,1,ちばさえこ,千葉千恵巳,,1,ちばちえみ,茅原実里,,1,ちはらみのり,月宮みどり,,1,つきみやみどり,辻あゆみ,,1,つじあゆみ,津田匠子,,1,つだしょうこ,津田美波,,1,つだみなみ,恒松あゆみ,,1,つねまつあゆみ,津野田なるみ,,1,つのだなるみ,津村まこと,,1,つむらまこと,鶴ひろみ,,1,つるひろみ,手塚ちはる,,1,てづかちはる,寺崎裕香,,1,てらさきゆか,寺田はるひ,,1,てらだはるひ,土井美加,,1,どいみか,冬馬由美,,1,とうまゆみ,東山奈央,,1,とうやまなお,富樫美鈴,,1,とがしみすず,徳井青空,,1,とくいそら,徳永愛,,1,とくながあい,戸田恵子,,1,とだけいこ,戸松遥,,1,とまつはるか,富沢美智恵,,1,とみざわみちえ,冨永みーな,,1,とみながみいな,豊口めぐみ,,1,とよぐちめぐみ,豊崎愛生,,1,とよさきあき,頓宮恭子,,1,とんぐうきょうこ" +
			",永井幸子,,1,ながいさちこ,中尾衣里,,1,なかおえり,中川亜紀子,,1,なかがわあきこ,中川里江,,1,なかがわりえ,長沢美樹,,1,ながさわみき,中島愛,_(声優),1,なかじまあい,中嶋アキ,,1,なかじまあき,中島沙樹,,1,なかじまさき,中嶋ヒロ,,1,なかじまひろ,永島由子,,1,ながしまゆうこ,永田依子,,1,ながたよりこ,中原麻衣,,1,なかはらまい,永見はるか,,1,ながみはるか,中村繪里子,,1,なかむらえりこ,中村桜,,1,なかむらさくら,中村千絵,,1,なかむらちえ,中村知子,,1,なかむらともこ,中山さら,,1,なかやまさら,名塚佳織,,1,なづかかおり,夏樹リオ,,1,なつきりお,生天目仁美,,1,なばためひとみ,並木のり子,,1,なみきのりこ,ならはしみき,,1,ならはしみき,成田紗矢香,,1,なりたさやか,南條愛乃,,1,なんじょうよしの,ニーコ,,1,にいこ,新名彩乃,,1,にいなあやの,仁後真耶子,,1,にごまやこ,西墻由香,,1,にしがきゆか,西原久美子,,1,にしはらくみこ,西村ちなみ,,1,にしむらちなみ,沼倉愛美,,1,ぬまくらまなみ,根谷美智子,,1,ねやみちこ" +
			",野川さくら,,1,のがわさくら,野沢雅子,,1,のざわまさこ,野田順子,,1,のだじゅんこ,野浜たまこ,,1,のはまたまこ,能登麻美子,,1,のとまみこ,野中藍,,1,のなかあい,野水伊織,,1,のみずいおり,野村道子,,1,のむらみちこ,朴ロ美,,1,ぱくろみ,橋本まい,,1,はしもとまい,長谷川明子,,1,はせがわあきこ,長谷優里奈,,1,はせゆりな,葉月絵理乃,,1,はづきえりの,花澤香菜,&i=1,1,はなざわかな,花村怜美,,1,はなむらさとみ,林原めぐみ,,1,はやしばらめぐみ,葉山いくみ,,1,はやまいくみ,早見沙織,,1,はやみさおり,早水リサ,,1,はやみずりさ,原えりこ,,1,はらえりこ,原紗友里,,1,はらさゆり,原田ひとみ,,1,はらだひとみ,原由実,,1,はらゆみ,潘恵子,,1,はんけいこ,潘めぐみ,,1,はんめぐみ,半場友恵,,1,はんばともえ" +	
			",比嘉久美子,,1,ひがくみこ,日笠陽子,,1,ひかさようこ,氷上恭子,,1,ひかみきょうこ,久川綾,,1,ひさかわあや,日高奈留美,,1,ひだかなるみ,日高のり子,,1,ひだかのりこ,日高里菜,,1,ひだかりな,ひと美,,1,ひとみ,日野未歩,,1,ひのみほ,日野由利加,,1,ひのゆりか,氷青,,1,ひょうせい,兵藤まこ,,1,ひょうどうまこ,平井理子,,1,ひらいりこ,平田宏美,,1,ひらたひろみ,平田真菜,,1,ひらたまな,平野綾,&i=1,1,ひらのあや,平野文,,1,ひらのふみ,平松晶子,,1,ひらまつあきこ,廣田詩夢,,1,ひろたしおん,広橋涼,,1,ひろはしりょう,深見梨加,,1,ふかみりか,福井裕佳梨,&i=1,1,ふくいゆかり,福圓美里,,1,ふくえんみさと,福原香織,,1,ふくはらかおり,藤田咲,,1,ふじたさき,藤田淑子,,1,ふじたとしこ,藤東知夏,,1,ふじとうちか,藤村歩,,1,ふじむらあゆみ,渕上舞,,1,ふちがみまい,渕崎ゆり子,,1,ふちざきゆりこ" +
			",洞内愛,,1,ほらないあい,堀江美都子,,1,ほりえみつこ,堀江由衣,,1,ほりえゆい,堀中優希,,1,ほりなかゆき,本田貴子,,1,ほんだたかこ,本多知恵子,,1,ほんだちえこ,本多真梨子,,1,ほんだまりこ,本多陽子,,1,ほんだようこ,本名陽子,&i=1,1,ほんなようこ,前田愛,_(声優),1,まえだあい,まきいづみ,,1,まきいづみ,牧口真幸,,1,まきぐちまゆき,牧島有希,,1,まきしまゆき,牧野由依,,1,まきのゆい,MAKO,,1,まこ,真柴摩利,,1,ましばまり,増田ゆき,,1,ますだゆき,升望,,1,ますのぞみ,増山江威子,,1,ますやまえいこ,又吉愛,,1,またよしあい,松井菜桜子,,1,まついなおこ,松浦チエ,,1,まつうらちえ,松岡由貴,,1,まつおかゆき,松岡洋子,_(声優),1,まつおかようこ,松来未祐,,1,まつきみゆ,松嵜麗,,1,まつざきれい,松下こみな,,1,まつしたこみな,松島みのり,,1,まつしまみのり,松本さち,,1,まつもとさち,松元惠,,1,まつもとめぐみ,松本梨香,,1,まつもとりか,的井香織,,1,まといかおり,間宮くるみ,,1,まみやくるみ,丸尾知子,,1,まるおともこ" +
			",三上枝織,,1,みかみしおり,味里,,1,みさと,三澤紗千香,,1,みさわさちか,水樹奈々,,1,みずきなな,水沢史絵,,1,みずさわふみえ,水谷優子,,1,みずたにゆうこ,水田わさび,,1,みずたわさび,水野愛日,,1,みずのまなび,水野理紗,,1,みずのりさ,水橋かおり,,1,みずはしかおり,水原薫,,1,みずはらかおる,三田ゆう子,,1,みたゆうこ,三石琴乃,,1,みついしことの,三橋加奈子,,1,みつはしかなこ,皆川純子,,1,みながわじゅんこ,皆口裕子,,1,みなぐちゆうこ,南央美,,1,みなみおみ,水杜明寿香,,1,みなもりあすか,三森すずこ,,1,みもりすずこ,宮川美保,,1,みやがわみほ,三宅華也,,1,みやけかや,宮崎羽衣,,1,みやざきうい,宮寺智子,,1,みやでらともこ,宮村優子,_(声優),1,みやむらゆうこ,武藤礼子,,1,むとうれいこ,村井かずさ,,1,むらいかずさ,望月久代,,1,もちづきひさよ,桃井はるこ,,1,ももいはるこ,森永理科,,1,もりながりか,茂呂田かおる,,1,もろたかおる,諸星すみれ,,1,もろぼしすみれ" +
			",矢島晶子,,1,やじまあきこ,弥永和子,,1,やながかずこ,矢作紗友里,,1,やはぎさゆり,山岡ゆり,,1,やまおかゆり,山像かおり,,1,やまがたかおり,山口眞弓,,1,やまぐちまゆみ,山口由里子,,1,やまぐちゆりこ,山崎和佳奈,,1,やまざきわかな,山田栄子,,1,やまだえいこ,山田みほ,,1,やまだみほ,山本彩乃,,1,やまもとあやの,山本圭子,,1,やまもとけいこ,山本希望,,1,やまもとのぞみ,山本麻里安,,1,やまもとまりあ,山本百合子,,1,やまもとゆりこ,弥生みつき,,1,やよいみつき,悠木碧,&i=1,1,ゆうきあおい,ゆかな,,1,ゆかな,雪野五月,,1,ゆきのさつき,柚木涼香,,1,ゆずきりょうか,湯屋敦子,,1,ゆやあつこ,ゆりん,,1,ゆりん" +
			",よこざわけい子,,1,よこざわけいこ,横山智佐,,1,よこやまちさ,吉田小百合,,1,よしださゆり,吉田聖子,,1,よしだせいこ,吉田理保子,,1,よしだりほこ,米澤円,,1,よねざわまどか,力丸乃りこ,,1,りきまるのりこ,渡辺明乃,,1,わたなべあけの,渡辺久美子,,1,わたなべくみこ,渡辺菜生子,,1,わたなべなおこ,渡辺美佐,_(声優),1,わたなべみさ"
			;

	public void doGet(HttpServletRequest req, HttpServletResponse resp)
			throws IOException {
		StringBuilder sb = new StringBuilder();
		sb.append("<html><head><meta http-equiv=\"content-type\" content=\"text/html; charset=UTF-8\" /></head><body><form action=\"\" method=\"POST\"><input type=\"text\" name=\"op\" value=\"\"/><input type=\"submit\" value=\"送信\"></form></body></html>");
		resp.setCharacterEncoding("utf-8");
		resp.setContentType("text/html");
		resp.getWriter().println(new String(sb));
	}

	public void doPost(HttpServletRequest req, HttpServletResponse resp)
			throws IOException {
		resp.setCharacterEncoding("utf-8");
		resp.setContentType("text/html");
		if(req.getParameter("op").split(",")[0].equals("recclear")){
			Cache cache = WikiUtil.getCache(60 * 60 * 24 * 365);
			cache.put(req.getParameter("op").split(",")[1] + ",record","");
		}
		if(req.getParameter("op").equals("newpast")){
			newpastQueue();
		}else if(req.getParameter("op").equals("pastCrowl")){
			pastCrowl();
		}else 		if(req.getParameter("op").equals("pastCrowl2")){
			pastCrowl2();
		}else if(req.getParameter("op").equals("crowler")){
			String now = req.getParameter("now");
			if(now == null)now = namelist.split(",")[1];
			crowler(now);
		}
		else if (req.getParameter("op").equals("newcolumn")) {
			resp.getWriter()
					.println(
							"<div id=\"column\"><select size=\"20\" onchange=\"ChangeSelection(this.form, this)\">");
			resp.getWriter().println(newColumn());
			resp.getWriter().println("</select></div>");

		} else if (req.getParameter("op").equals("memset")) {
			memSet();
		} else if (req.getParameter("op").equals("addcolumn"))
			addColumn();
		else if (req.getParameter("op").equals("rep"))
			replacePast();
		else if (req.getParameter("op").equals("addProperty"))
			addProperty();
		else if ((req.getParameter("op").split(",")[0]).equals("erase")) {
			DatastoreService datastore = DatastoreServiceFactory
					.getDatastoreService();
			Entity past_string = null;
			try {
				Key key = KeyFactory.createKey("past_string", "onlyOne");
				past_string = datastore.get(key);
			} catch (Throwable t) {
			}
			String past_text = ((Text) past_string.getProperty("text"))
					.getValue();
			resp.getWriter().println(past_text);
			String eraseName = req.getParameter("op").split(",")[1];
			Pattern p = Pattern.compile("^(.*)," + eraseName
					+ ",[^,]+,[0-9]+,[0-9]+(.*)$");
			Matcher m = p.matcher(past_text);
			while (m.find()) {
				past_text = m.group(1) + m.group(2);
				m = p.matcher(past_text);
			}
			p = Pattern.compile("^(.*),[^,]+," + eraseName
					+ ",[0-9]+,[0-9]+(.*)$");
			m = p.matcher(past_text);
			while (m.find()) {
				past_text = m.group(1) + m.group(2);
				m = p.matcher(past_text);
			}
			past_string.setProperty("text", new Text(past_text));
			Cache cache = WikiUtil.getCache(60*60*24);
			cache.put("past_string",past_text);
			if(false)datastore.put(past_string);
		} else {
			DatastoreService datastore = DatastoreServiceFactory
					.getDatastoreService();
			Entity past_string = null;
			try {
				Key key = KeyFactory.createKey("past_string", "onlyOne");
				past_string = datastore.get(key);
			} catch (Throwable t) {
			}
			if (req.getParameter("op").equals("clear")) {
				memSet();
				newColumn();
				StringBuilder sb = new StringBuilder();
				String[] namelist_array = namelist.split(",");
				for (int i = 0; i < namelist_array.length; i++) {
					if ((i + 1) % 4 > 1) {
						sb.append("," + namelist_array[i]);
					}
				}
				past_string
						.setProperty("back_string", new Text(new String(sb)));
				if(false)datastore.put(past_string);
				resp.getWriter().println(new String(sb));
			}

			for (int i = 0; i < 100; i++) {
				String now_string = ((Text) past_string
						.getProperty("back_string")).getValue();
				String[] back_strings = now_string.split(",", 4);
				now_string = "," + back_strings[3] + "," + back_strings[1]
						+ "," + back_strings[2];
				Queue queue = QueueFactory.getDefaultQueue();
				queue.add(Builder.withUrl(
						"/co?n=" + back_strings[1] + back_strings[2]
								+ "&c=this").method(Method.GET));
				past_string.setProperty("back_string", new Text(now_string));
				if(false)datastore.put(past_string);
			}

		}
	}

	public void addColumn() {
		DatastoreService datastore = DatastoreServiceFactory
				.getDatastoreService();
		Entity past_string = null;
		try {
			past_string = datastore.get(KeyFactory.createKey("past_string",
					"onlyOne"));
		} catch (EntityNotFoundException e) {
		}
		String[] string_box = ((Text) past_string.getProperty("text"))
				.getValue().split(",");
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i * 4 + 4 < string_box.length; i++) {
			sb.append("," + string_box[i * 4 + 1])
					.append("," + string_box[i * 4 + 2])
					.append("," + string_box[i * 4 + 3])
					.append("," + string_box[i * 4 + 4] + ",");
		}
		past_string.setProperty("past_text", new Text(new String(sb)));
		if(false)datastore.put(past_string);
	}

	public void replacePast() {
		DatastoreService datastore = DatastoreServiceFactory
				.getDatastoreService();
		Entity past_string = null;
		try {
			past_string = datastore.get(KeyFactory.createKey("past_string",
					"onlyOne"));
		} catch (EntityNotFoundException e) {
		}
		String[] string_box = ((Text) past_string.getProperty("text"))
				.getValue().split(",");
		StringBuilder sb = new StringBuilder();
		Pattern p = Pattern.compile("　");
		for (int i = 0; i * 4 + 4 < string_box.length; i++) {
			if (p.matcher(string_box[i * 4 + 1]).find()
					|| p.matcher(string_box[i * 4 + 2]).find())
				continue;
			sb.append("," + string_box[i * 4 + 1])
					.append("," + string_box[i * 4 + 2])
					.append("," + string_box[i * 4 + 3])
					.append("," + string_box[i * 4 + 4]);
		}
		past_string.setProperty("text", new Text(new String(sb)));
		if(false)datastore.put(past_string);
	}

	public String newColumn() {
		String[] hiragana = { "あ", "か", "さ", "た", "な", "は", "ま", "や", "あ", "か",
				"さ", "た", "な", "ぱ", "ま", "や", "" };
		String sx = "<option style=\"color:blue\">- 男性 ";
		String[] string_box = namelist.split(",");
		StringBuilder sb = new StringBuilder();
		sb.append("<option>リスト選択</option>");
		int j = 0;
		for (int i = 0; i * 4 + 4 < string_box.length; i++) {
			if (hiragana[j].equals(string_box[i * 4 + 4].replaceAll("^(.).*$",
					"$1"))) {
				if (j == 8) {
					sx = "<option style=\"color:red\">- 女性 ";
				}
				if (hiragana[j].equals("ぱ"))
					hiragana[j] = "は";
				if (hiragana[j].equals("や")) {
					sb.append(sx + " や・わ -</option>\n");
				} else
					sb.append(sx + hiragana[j] + "行 -" + "</option>\n");
				j++;
			}
			sb.append("<option value=\"" + string_box[i * 4 + 1] + "\">"
					+ string_box[i * 4 + 1] + "</option>\n");
		}
		WikiUtil.getCache(60 * 60 * 24 * 365).put("namecolumn", new String(sb));
		return new String(sb);
	}

	public void memSet() {
		WikiUtil.getCache(60 * 60 * 24 * 365).put("namelist", namelist);
	}

	public void addProperty() {
		DatastoreService datastore = DatastoreServiceFactory
				.getDatastoreService();
		Entity past_string = null;
		try {
			Key key = KeyFactory.createKey("past_string", "onlyOne");
			past_string = datastore.get(key);
		} catch (Throwable t) {
		}
		past_string.setProperty("past_update", new String(""));
		if(false)datastore.put(past_string);
	}

	public void crowler(String s) {
		String next_op;
		String next;
		try{
			next = namelist.split("," + s +",[^,]*,-?1,[^,]*")[1];
			next_op = next.split(",")[2];
			next = next.split(",")[1];
		} catch(Throwable t){
			next = namelist.split(",")[1];
			next_op = namelist.split(",")[2];
		}
		Queue queue = QueueFactory.getDefaultQueue();
		queue.add(Builder.withUrl(
				"/co?n=" + next + next_op
						+ "&c=this").method(Method.GET).countdownMillis(45000));
		queue.add(Builder.withUrl(
				"/back").param("op", "crowler").param("now", next)
				.method(Method.POST).countdownMillis(90000));
	}

	public void pastCrowl(){
		DatastoreService datastore = DatastoreServiceFactory
				.getDatastoreService();
		Entity past_string = null;
		try {
			Key key = KeyFactory.createKey("past_string", "onlyOne");
			past_string = datastore.get(key);
		} catch (Throwable t) {
		}
		past_string.setProperty("text",new Text(new Past().getPastString()));
		try{
		if(false)datastore.put(past_string);
		}catch(Throwable t){}
		Queue queue = QueueFactory.getDefaultQueue();
		queue.add(Builder.withUrl("/back").param("op","pastCrowl").method(Method.POST).countdownMillis(60000*30));
		queue.add(Builder.withUrl("/sync").param("mode", "past").method(Method.GET).countdownMillis(30000));
		queue.add(Builder.withUrl("/sync").param("mode", "ranking").method(Method.GET).countdownMillis(45000));
		queue.add(Builder.withUrl("/sync").param("mode", "pair").method(Method.GET).countdownMillis(60000));
		queue.add(Builder.withUrl("/ranking").param("c", "true").method(Method.GET).countdownMillis(90000));
		queue.add(Builder.withUrl("/pair").param("c", "true").method(Method.GET).countdownMillis(120000));
	}

	public void pastCrowl2(){
		DatastoreService datastore = DatastoreServiceFactory
				.getDatastoreService();
		Entity past_string = null;
		try {
			Key key = KeyFactory.createKey("past_string", "onlyOne");
			past_string = datastore.get(key);
		} catch (Throwable t) {
		}
		past_string.setProperty("text",new Text(new Past().getPastString()));
		try{
		//if(false)
			datastore.put(past_string);
		}catch(Throwable t){}
		Queue queue = QueueFactory.getDefaultQueue();
		queue.add(Builder.withUrl("/back").param("op","pastCrowl2").method(Method.POST).countdownMillis(60000*60));
		queue.add(Builder.withUrl("/ranking").param("c","this").method(Method.GET).countdownMillis(60000*50));
		queue.add(Builder.withUrl("/pair").param("c","this").method(Method.GET).countdownMillis(60000*40));
	}
	
	public void newpastQueue(){
		Queue queue = QueueFactory.getDefaultQueue();
		queue.add(Builder.withUrl("/newpast").method(Method.GET));
	}
}