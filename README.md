# GooglePay_aar_forUnity
GooglePay_aar_forUnity


此工程功能是在AS中接入Google支付，然后生成aar文件给Unity调用，

对应的Unity工程是：https://github.com/amylaogong/Unity_IAP_Google_With_aarCreatedByAS


环境：

AS:3.3.2

Unity:2018.1.6



==================================================================================================

将本工程download到本地，用AS打开，调整好工程可以正常build，经测试遇到的坑，全是翻墙网络原因导致build失败

翻墙正常也可能因为不稳定而出现各种问题，这个过程本人是一直在try again，终于过了这一步




然后打开工程 AndroidStudio里Terminal里执行命令： gradlew googlepay:assembleRelease

然后会在.\googlepay\build\outputs\aar目录下生成aar文件：googlepay-release.aar




用压缩软件打开，删掉aar文件内libs下的jar包，因为跟unity自带的会重复，unity打包的时候会自动引用自身的classes.jar

之后将最终的aar文件复制到unity工程安卓插件路径下面：

\Assets\Plugins\Android\



配置unity打包信息，跟Googleplay后台一致，包括包名，versioncode，versionname，应用签名等


最后用unity打出安卓包，测试即可



==================================================================================================



代码介绍：



MainActivity.java,提供Unity里面调用的接口

unity调用安卓代码：

mainActivity.Call("SetRunMode",runMode);//运行模式，可以切换测试环境

mainActivity.Call("InitPay",googleKey);//开启支付的初始化操作

mainActivity.Call("ChargeByProductID", productID);//支付

mainActivity.Call("QuerySkuOnwed");//查询



SetRunMode（bool），可以设置是否为测试模式，本例测试模式下只是打开了log开关

InitPay（string） ,从unity传入googleKey，初始化支付

ChargeByProductID（string） 通过支付档位id，进行支付

QuerySkuOnwed（），请求拥有的物品进行消耗，也用于开始时的检查，如果有则会执行消耗逻辑






安卓回调unity的接口：OnNotifyWithJson（）

返回json字符串，Unity通过获取function字段的不同，来判断应该执行的逻辑

Unity里的json序列化类成员：

public class IAPCallBackResult{

	public string function;//回调方法
	
	public int code = -1;//状态
	
	public int ownedSkuSize = -1;//拥有未消耗的物品数量
	
	public int IabHelerCode;//
	
	public string resultMsg;//Google返回的订单说明
	

	public string googleOrderId;//getOrderId，google订单号
	
	public string getSku;//该笔支付的档位id
	
	public string selfOrderId;//getDeveloperPayload，向google下单时的自定义订单号
	
	

	public string getPurchaseTime;
	
	public string getItemType;
	
	public string getOriginalJson;
	
	public string getPackageName;
	
	public string getPurchaseState;
	
	public string getSignature;
	
	public string getToken;
	

	public string key;//
	
	public string value;//


	//预留6个档位吧，一般4个就够用了
	
	public string getSku_1;//
	
	public string getSku_2;//
	
	public string getSku_3;//
	
	public string getSku_4;//
	
	public string getSku_5;//
	
	public string getSku_6;//
	
	
}

code取值：

public enum IAP_PAY_STATE{

	PAY_STATE_CONSUME_SUCCESS = 0,//消费成功
	
	PAY_STATE_SERVICE_INIT_FAILED = 1998,//初始化失败
	
	PAY_STATE_SERVICE_READY = 1999,//初始化完毕，服务准备完毕
	
	PAY_STATE_PROCESS_PURCHASE_CANCELLED=2001,//用户取消
	
	PAY_STATE_PROCESS_PURCHASE=2000,//purchase过程中，再根据IapHeper的状态码判断进度
	
	PAY_STATE_PROCESS_PURCHASE_DONE=2002,//购买完成，purcahse over支付完成还未消耗
	
	PAY_STATE_PROCESS_CONSUME=3000//消费有了回调，然后判断是否成功，成功之后就调用另一个方法了OnCallBackPaySuccess
	
}



function取值如下：

OnCallBackPayProcess，主要是支付从初始化到最终成功前的状态判断

OnCallBackPaySuccess，支付所有流程完毕，该回调指的是成功消耗物品之后的回调

OnCallBackQuerryOwnedSku，查询google服务器上拥有的未消耗的物品

SetCache，设置字典映射，json中包含key，value字段





==================================================================================================




AndroidJavaClass jc = new AndroidJavaClass("com.example.helloworld.GooglePayActivity");

jc.CallStatic("TestStaticCall","AndroidInterface.cs_com.example.helloworld.GooglePayActivity");

jc = new AndroidJavaClass("com.unity.callback.AndroidUnityInterface");

jc.CallStatic("SetUnityCache","fromWhere","com.unity.callback.AndroidUnityInterface");



unity相关代码

using System.Collections;

using System.Collections.Generic;

using UnityEngine;

public class AndroidInterface : MonoBehaviour {

	// Use this for initialization
	
	void Start () {
		
	}
	
	// Update is called once per frame
	
	void Update () {
		
	}

	private static AndroidJavaObject mainActivity;
	
	private static string googlePayKey = "you google public key";
	
	private static int runMode = 0;// 0 正常；1测试,测试状态下会打开log
	
	private static string googleSingInKey = "328571750760-gfgle265k00ou5hqp1knegsjcc5desbf.apps.googleusercontent.com";


	public static void SetViewLog(string str)
	{
	
		LogView.setViewText ("SetViewLog,str=="+str);
		
	}


	#if UNITY_ANDROID


	public static void SetRunMode()
	{
	
		AndroidJavaClass jc = new AndroidJavaClass("com.unity3d.player.UnityPlayer");
		
		mainActivity = jc.GetStatic<AndroidJavaObject>("currentActivity");

		if (runMode.Equals(0)) {
		
			runMode = 1;
			
		} else {
		
			runMode = 0;
			
		}
		
		LogView.setViewText ("AndroidInterface.cs,SetRunMode,Unity call Java...runMode=="+runMode);
		
		mainActivity.Call("SetRunMode",runMode);
		
	}


	public static void InitPay()
	
	{
		LogView.setViewText ("AndroidInterface.cs,InitPay,Unity call Java...InitPay");

		AndroidJavaClass jc = new AndroidJavaClass("com.unity3d.player.UnityPlayer");
		
		mainActivity = jc.GetStatic<AndroidJavaObject>("currentActivity");

		mainActivity.Call("InitPay",googlePayKey);
		
	}


	public static void ChargeByProductID(string productID)
	
	{
		AndroidJavaClass jc = new AndroidJavaClass("com.unity3d.player.UnityPlayer");
		
		mainActivity = jc.GetStatic<AndroidJavaObject>("currentActivity");

		LogView.setViewText ("AndroidInterface,ChargeWithProductID,Unity call Java...productID=="+productID);
		
		mainActivity.Call("ChargeByProductID", productID);

		//Test ();

	}


	public static void QuerySkuOnwed()
	{
	
		LogView.setViewText ("AndroidInterface.cs,222QuerySkuOnwed,Unity call Java...QuerySkuOnwed");

		AndroidJavaClass jc = new AndroidJavaClass("com.unity3d.player.UnityPlayer");
		
		mainActivity = jc.GetStatic<AndroidJavaObject>("currentActivity");

		mainActivity.Call("QuerySkuOnwed");
	}
	

	public static void DoLogin(string account,string passwd){
	
		LogView.setViewText ("AndroidInterface.cs,222DoLogin,Unity call Java...DoLogin");
		
		AndroidJavaClass jc = new AndroidJavaClass("com.unity3d.player.UnityPlayer");
		
		mainActivity = jc.GetStatic<AndroidJavaObject>("currentActivity");

		mainActivity.Call("DoLogin",account,passwd);

	}

	public static void Test(){
	
		LogView.setViewText ("AndroidInterface.cs,Test...");
		
		AndroidJavaClass jc = new AndroidJavaClass("com.example.helloworld.MainActivity");
		
		jc.CallStatic("TestStaticCall","AndroidInterface.cs_com.example.helloworld.MainActivity");

		jc = new AndroidJavaClass("com.unity.callback.AndroidUnityInterface");
		
		jc.CallStatic("SetUnityCache","fromWhere","com.unity.callback.AndroidUnityInterface");
		

	}


	public static void SDKLogin(int mode)
	{
	
		LogView.setViewText ("AndroidInterface.cs,SDKLogin,Unity call Java...SDKLogin");

		AndroidJavaClass jc = new AndroidJavaClass("com.unity3d.player.UnityPlayer");
		
		mainActivity = jc.GetStatic<AndroidJavaObject>("currentActivity");

		mainActivity.Call("SDKLogin",mode,googleSingInKey);
		
	}

	public static void SDKLogout(int mode)
	{
	
		LogView.setViewText ("AndroidInterface.cs,SDKLogout,Unity call Java...SDKLogout");

		AndroidJavaClass jc = new AndroidJavaClass("com.unity3d.player.UnityPlayer");
		
		mainActivity = jc.GetStatic<AndroidJavaObject>("currentActivity");

		mainActivity.Call("SDKLogout",mode);
	}

	#endif

	//被安卓回调的方法。。。
	public void OnNotifyWithJson(string jsonStr)
	{
	
		//通过返回的function字段判定走相应的逻辑，
		
		//OnCallBackPayProcess,OnCallBackPaySuccess,OnCallBackQuerryOwnedSku,SetCache


		IAPGoogle.IAPCallback (jsonStr);
	}

}





