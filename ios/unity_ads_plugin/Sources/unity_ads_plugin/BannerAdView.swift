import Flutter
import UnityAds

class BannerAdView: NSObject, FlutterPlatformView, UADSBannerViewDelegate {
    private let bannerView : UADSBannerView
    private let uiView : UIView
    private let listener : BannerAdListener
    
    init(frame: CGRect, id: Int64, arguments: Any?, messenger: FlutterBinaryMessenger) {
        let args = arguments as! [String: Any]? ?? [:]
        let placementId =  args[UnityAdsConstants.PLACEMENT_ID_PARAMETER] as? String ?? ""
        
        let width =  args[UnityAdsConstants.WIDTH_PARAMETER] as? CGFloat
        let height = args[UnityAdsConstants.HEIGHT_PARAMETER] as? CGFloat
        let size = CGSize(width: width ?? 320.0, height: height ?? 50.0)
        bannerView = UADSBannerView.init(placementId: placementId, size: size)
        
        let channel =  FlutterMethodChannel(
            name: UnityAdsConstants.BANNER_AD_CHANNEL + "_" + id.description,  binaryMessenger:  messenger)
        listener = BannerAdListener(channel: channel)
        
        uiView = UIView(frame: frame)
        super.init()
        
        bannerView.delegate = listener
        
        bannerView.load()
        uiView.addSubview(bannerView)
        uiView.layoutIfNeeded()
    }
    
    func view() -> UIView {
        uiView
    }

    /// VIBE ZO FORK vs upstream unity_ads_plugin 0.4.0, which has no teardown
    /// here at all.
    ///
    /// `UADSBannerView` exposes no `destroy()` (unlike its Android
    /// counterpart) — it is a plain `UIView` that stops its refresh cycle when
    /// it deallocates. Upstream relies entirely on ARC for that, which only
    /// holds while nothing else retains the view. This makes the teardown
    /// explicit and deterministic: drop it out of the view hierarchy and clear
    /// the delegate so a late SDK callback can never reach a
    /// `FlutterMethodChannel` whose Dart side is already gone.
    ///
    /// `delegate` is declared `weak` on `UADSBannerView`, so `listener` (held
    /// strongly by this object) must not be released before the delegate is
    /// cleared — hence the explicit nil rather than leaving it to deinit
    /// ordering.
    deinit {
        bannerView.delegate = nil
        bannerView.removeFromSuperview()
    }
}

