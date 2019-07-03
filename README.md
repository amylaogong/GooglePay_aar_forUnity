# GooglePay_aar_forUnity
GooglePay_aar_forUnity

打开工程 AndroidStudio里Terminal里执行命令： gradlew googlepay:assembleDebug


然后会在.\googlepay\build\outputs\aar目录下生成aar文件：googlepay-debug.aar


用压缩软件打开，删掉aar文件内libs下的jar包，是unity自带的会重复



代码中安卓调用unity的方法有：


OnCallBackPayProcess，支付流程进度，json返回状态码，code，对应数值：


public static int PAY_STATE_PROCESS_PURCHASE = 2000;//purchase过程中，再根据IapHeper的状态码判断进度


public static int PAY_STATE_PROCESS_PURCHASE_CANCELLED = 2001;//用户取消支付


public static int PAY_STATE_PROCESS_PURCHASE_DONE = 2002;//purcahse over支付完成还未消耗


public static int PAY_STATE_PROCESS_CONSUME = 3000;//consume开始消费，成功之后就调用另一个方法了OnCallBackPaySuccess



OnCallBackPaySuccess，支付所有流程完毕，该回调指的是成功消耗物品之后的回调


返回json格式，googleOrderId字段是google订单号，getSku是该笔支付的档位id，selfOrderId字段是向google下单时的自定义订单号



OnCallBackQuerryOwnedSku，查询google服务器上拥有的未消耗的物品

code状态码

ownedSkuSize物品数量

getSku_{i},物品sku





unity调用安卓代码：

mainActivity.Call("SetRunMode",runMode);//运行模式，可以切换测试环境

mainActivity.Call("InitPay",googleKey);//开启支付的初始化操作

mainActivity.Call("ChargeByProductID", productID);//支付

mainActivity.Call("QuerySkuOnwed");//查询





unity相关代码


using System.Collections; using System.Collections.Generic; using UnityEngine;

public class AndroidInterface : MonoBehaviour {

// Use this for initialization
void Start () {
	
}

// Update is called once per frame
void Update () {
	
}

private static AndroidJavaObject mainActivity;
private static string googleKey = "MIIBIjANBgkqhkiG9w0********";//此处是设置的Google Public Key
private static int runMode = 0;// 0 正常；1测试


public static void SetViewLog(string str)
{
	LogView.setViewText ("SetViewLog,str=="+str);
}

public static void SetRunMode()
{
	AndroidJavaClass jc = new AndroidJavaClass("com.unity3d.player.UnityPlayer");
	mainActivity = jc.GetStatic<AndroidJavaObject>("currentActivity");

	if (runMode.Equals(0)) {
		runMode = 1;
	} else {
		runMode = 0;
	}
	LogView.setViewText ("SetRunMode,Unity call Java...runMode=="+runMode);
	mainActivity.Call("SetRunMode",runMode);
}


public static void InitPay()
{
	LogView.setViewText ("InitPay,Unity call Java...InitPay");

	AndroidJavaClass jc = new AndroidJavaClass("com.unity3d.player.UnityPlayer");
	mainActivity = jc.GetStatic<AndroidJavaObject>("currentActivity");

	mainActivity.Call("InitPay",googleKey);
}


public static void ChargeByProductID(string productID)
{
	AndroidJavaClass jc = new AndroidJavaClass("com.unity3d.player.UnityPlayer");
	mainActivity = jc.GetStatic<AndroidJavaObject>("currentActivity");

	LogView.setViewText ("AndroidInterface,ChargeWithProductID,Unity call Java...productID=="+productID);
	mainActivity.Call("ChargeByProductID", productID);
}

public static void QuerySkuOnwed()
{
	LogView.setViewText ("222QuerySkuOnwed,Unity call Java...QuerySkuOnwed");

	AndroidJavaClass jc = new AndroidJavaClass("com.unity3d.player.UnityPlayer");
	mainActivity = jc.GetStatic<AndroidJavaObject>("currentActivity");

	mainActivity.Call("QuerySkuOnwed");
}

//被安卓回调的方法不能使用静态方法。。。

public void OnCallBackPayProcess(string msg)

{

	LogView.setViewText ("OnCallBackPayProcess,msg=="+msg);
	
	SetViewLog (msg);
	
}


public void OnCallBackPaySuccess(string msg)


{


	LogView.setViewText ("22OnCallBackPaySuccess,msg=="+msg);
	
	
	SetViewLog (msg);
	
	
}
public void OnCallBackQuerryOwnedSku(string msg)

{

	LogView.setViewText ("22OnCallBackQuerryOwnedSku,msg=="+msg);
	
	SetViewLog (msg);
	
}

