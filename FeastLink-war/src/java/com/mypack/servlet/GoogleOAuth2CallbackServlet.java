package com.mypack.servlet;

import com.mypack.entity.Users;
import com.mypack.service.GoogleOAuth2Service;
import com.mypack.sessionbean.UsersFacadeLocal;

import jakarta.ejb.EJB;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import org.mindrot.jbcrypt.BCrypt;

@WebServlet(name = "GoogleOAuth2CallbackServlet", urlPatterns = {"/google-oauth-callback"})
public class GoogleOAuth2CallbackServlet extends HttpServlet {

    // ✅ Web OAuth Client
    private static final String GOOGLE_CLIENT_ID =
            "718278911834-kbcfe7nbb79b263qdq0ha1vscven73kb.apps.googleusercontent.com";

    private static final String GOOGLE_CLIENT_SECRET =
            "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";

    // ✅ MUST match Google Console redirect URI exactly
    private static final String REDIRECT_URI =
            "http://localhost:8082/FeastLink-war/google-oauth-callback";

    @EJB
    private UsersFacadeLocal usersFacade;

    @EJB
    private GoogleOAuth2Service googleOAuth2Service;

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        handle(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        handle(request, response);
    }

    private void handle(HttpServletRequest request, HttpServletResponse response) throws IOException {

        // user canceled / google error
        String err = request.getParameter("error");
        if (err != null && !err.isBlank()) {
            response.sendRedirect(request.getContextPath() + "/faces/login.xhtml?gerr=cancel");
            return;
        }

        String code = request.getParameter("code");
        String state = request.getParameter("state");

        if (code == null || code.isBlank()) {
            response.sendRedirect(request.getContextPath() + "/faces/login.xhtml?gerr=missing_code");
            return;
        }

        // CSRF state check
        HttpSession pre = request.getSession(false);
        String expected = (pre != null) ? (String) pre.getAttribute("google_oauth_state") : null;
        if (expected == null || state == null || !expected.equals(state)) {
            response.sendRedirect(request.getContextPath() + "/faces/login.xhtml?gerr=state");
            return;
        }

        // consume state
        try { pre.removeAttribute("google_oauth_state"); } catch (Exception ignore) {}

        try {
            // 1) code -> token -> userinfo (SSL fixed inside service)
            GoogleOAuth2Service.GoogleUserInfo info =
                    googleOAuth2Service.getUserInfoFromCode(code, GOOGLE_CLIENT_ID, GOOGLE_CLIENT_SECRET, REDIRECT_URI);

            if (info == null || info.email == null || info.email.isBlank()) {
                response.sendRedirect(request.getContextPath() + "/faces/login.xhtml?gerr=userinfo");
                return;
            }

            String email = info.email.trim();
            String fullName = info.name;
            String picture = info.picture;

            // 2) map theo email
            Users matched = findUserByEmail(email);

            // 3) create new if not exists
            if (matched == null) {
                Users u = new Users();
                u.setEmail(email);
                u.setFullName((fullName != null && !fullName.isBlank()) ? fullName : email);
                u.setAvatarUrl(picture);

                u.setRole("CUSTOMER");
                u.setStatus("ACTIVE");
                u.setCreatedAt(new Date());

                String random = UUID.randomUUID().toString();
                u.setPassword(BCrypt.hashpw(random, BCrypt.gensalt()));

                try { u.setLastLoginAt(new Date()); } catch (Exception ignore) {}

                usersFacade.create(u);
                matched = u;
            } else {
                if ((matched.getAvatarUrl() == null || matched.getAvatarUrl().isBlank()) && picture != null) {
                    matched.setAvatarUrl(picture);
                }
                try { matched.setLastLoginAt(new Date()); } catch (Exception ignore) {}
                try { usersFacade.edit(matched); } catch (Exception ignore) {}
            }

            // 4) status check (giống login thường)
            if (matched.getStatus() != null && !"ACTIVE".equalsIgnoreCase(matched.getStatus())) {
                response.sendRedirect(request.getContextPath() + "/faces/login.xhtml?gerr=inactive");
                return;
            }

            // 5) reset session + set session giống login()
            HttpSession old = request.getSession(false);
            if (old != null) old.invalidate();

            HttpSession session = request.getSession(true);
            session.setAttribute("currentUser", matched);
            session.setAttribute("loginIdentifier", email);

            String role = matched.getRole();
            if (role != null && "MANAGER".equalsIgnoreCase(role)) {
                session.setAttribute("currentUserRole", "MANAGER");
                response.sendRedirect(request.getContextPath() + "/faces/Restaurant/dashboard.xhtml");
            } else if (role != null && "ADMIN".equalsIgnoreCase(role)) {
                session.setAttribute("currentUserRole", "ADMIN");
                response.sendRedirect(request.getContextPath() + "/faces/Admin/dashboard.xhtml");
            } else {
                session.setAttribute("currentUserRole", "CUSTOMER");
                // ✅ đúng yêu cầu: về Customer/index
                response.sendRedirect(request.getContextPath() + "/faces/Customer/index.xhtml");
            }

        } catch (Exception ex) {
            ex.printStackTrace();
            // ✅ nếu còn PKIX thì vẫn sẽ rơi vào đây, nhưng với Service bên trên thường sẽ hết
            response.sendRedirect(request.getContextPath() + "/faces/login.xhtml?gerr=ssl");
        }
    }

    private Users findUserByEmail(String email) {
        try {
            List<Users> all = usersFacade.findAll();
            for (Users u : all) {
                if (u.getEmail() != null && email.equalsIgnoreCase(u.getEmail())) {
                    return u;
                }
            }
        } catch (Exception ignore) {}
        return null;
    }
}
