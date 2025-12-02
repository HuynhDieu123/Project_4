package com.customer.bean;

import com.mypack.entity.Restaurants;
import com.mypack.sessionbean.RestaurantsFacadeLocal;
import jakarta.ejb.EJB;
import jakarta.enterprise.context.RequestScoped;
import jakarta.faces.context.FacesContext;
import jakarta.inject.Named;

import java.io.Serializable;
import java.util.Map;

@Named("restaurantChatBean")
@RequestScoped
public class RestaurantChatBean implements Serializable {

    @EJB
    private RestaurantsFacadeLocal restaurantsFacade;

    private Restaurants currentRestaurant;
    private String zaloLink;   // link Zalo cuối cùng

    // ================== INIT ==================
    public RestaurantChatBean() {
    }

    @jakarta.annotation.PostConstruct
    public void init() {
        try {
            FacesContext fc = FacesContext.getCurrentInstance();
            Map<String, String> params = fc.getExternalContext().getRequestParameterMap();

            // Thử lấy theo nhiều tên param khác nhau (tùy bạn đang dùng cái nào)
            String idParam = params.get("restaurantId");
            if (idParam == null || idParam.isBlank()) {
                idParam = params.get("id");
            }

            if (idParam != null && !idParam.isBlank()) {
                Long id = Long.valueOf(idParam);
                currentRestaurant = restaurantsFacade.find(id);
            }

            // Nếu tìm được nhà hàng thì build link Zalo từ phone
            if (currentRestaurant != null && currentRestaurant.getPhone() != null) {
                String normalized = normalizePhoneForZalo(currentRestaurant.getPhone());
                if (normalized != null && !normalized.isBlank()) {
                    zaloLink = "https://zalo.me/" + normalized;
                }
            }

            // Fallback (tuỳ bạn): nếu nhà hàng chưa có phone thì có thể dùng Zalo support chung
            // Nếu không muốn fallback, thì cứ để zaloLink = null là widget sẽ không render.
            /*
            if (zaloLink == null) {
                zaloLink = "https://zalo.me/84xxxxxxxxx"; // Zalo support chung của FeastLink
            }
            */

        } catch (Exception ex) {
            // Log nếu cần, nhưng đừng để crash trang
            ex.printStackTrace();
        }
    }

    // ================== HELPER ==================
    /**
     * Chuẩn hoá số điện thoại lưu trong DB:
     *  - Bỏ hết ký tự không phải số
     *  - Nếu bắt đầu bằng 0 -> chuyển thành 84 + phần còn lại
     *  - Nếu đã là 84xxxxxxxx thì giữ nguyên
     */
    private String normalizePhoneForZalo(String rawPhone) {
        if (rawPhone == null) return null;

        // Giữ lại chữ số
        String digits = rawPhone.replaceAll("\\D", "");
        if (digits.isEmpty()) return null;

        if (digits.startsWith("84")) {
            return digits; // đã đúng định dạng
        }

        if (digits.startsWith("0") && digits.length() >= 9) {
            return "84" + digits.substring(1);
        }

        // Trường hợp khác: vẫn trả về digits (ít nhất vẫn là số)
        return digits;
    }

    // ================== GETTER ==================
    public Restaurants getCurrentRestaurant() {
        return currentRestaurant;
    }

    public String getZaloLink() {
        return zaloLink;
    }

    public boolean isChatAvailable() {
        return zaloLink != null && !zaloLink.isBlank();
    }
}
