package kh.semi.servlet;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import kh.semi.dao.BoardDAO;
import kh.semi.dao.MemberDAO;
import kh.semi.dto.BoardDTO;
import kh.semi.dao.MemberDAO;
import kh.semi.dto.MemberDTO;

@WebServlet("*.members")
public class MembersController extends HttpServlet {
	protected void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		response.setCharacterEncoding("UTF-8");
		request.setCharacterEncoding("UTF-8");

		PrintWriter printWriter = response.getWriter();
		String reqUri = request.getRequestURI();
		String ctxPath = request.getContextPath();
		String cmd = reqUri.substring(ctxPath.length());
		MemberDAO dao = new MemberDAO();
		BoardDAO bdao = new BoardDAO();

		if (cmd.equals("/Main.members")) {
			List<BoardDTO> list;
			try {
				list = bdao.getDataForMain();
				request.setAttribute("list", list);
				for(int i = 0; i < list.size(); i++) {
					String[] strArr = new String[3];
					double[] douArr = new double[3];
					
					int goalAmount = list.get(i).getAmount();
					Timestamp dueDate = list.get(i).getDueDate();
					long dueTime = dueDate.getTime();
					SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
					strArr[i] = sdf.format(dueTime);
					int sumAmount = list.get(i).getSumAmount();
					douArr[i] = Math.floor((double)sumAmount / goalAmount * 100);
					if(i == list.size()-1) {
						request.setAttribute("duedate", strArr);
						request.setAttribute("percentage", douArr);
					}
				}
				request.getRequestDispatcher("main.jsp").forward(request, response);
			}catch(Exception e) {
				e.printStackTrace();
			}
		}else if(cmd.equals("/Introduce.members")) {
			request.getRequestDispatcher("/WEB-INF/basics/introduce.jsp").forward(request, response);
			
		}else if (cmd.equals("/JoinForm.members")) {
			request.getRequestDispatcher("/WEB-INF/basics/joinForm.jsp").forward(request, response);

		} else if (cmd.equals("/EmailDuplCheck.members")) {
			String email = request.getParameter("email");
			try {
				boolean result = dao.emailDuplCheck(email);
				printWriter.print(result);
			} catch (Exception e) {
				e.printStackTrace();
			}

		} else if (cmd.equals("/SendMail.members")) {
			String email = request.getParameter("email");
			try {
				int ranNum = dao.sendMail(email);
				printWriter.print(ranNum);
			} catch (Exception e) {
				e.printStackTrace();
			}

		} else if (cmd.equals("/Join.members")) {
			String email = request.getParameter("email");
			String pw = request.getParameter("pw");
			String name = request.getParameter("name");
			String phone = request.getParameter("phone");
			String zipCode = request.getParameter("zip");
			String address1 = request.getParameter("address1");
			String address2 = request.getParameter("address2");

			MemberDTO dto = new MemberDTO(email, pw, name, phone, zipCode, address1, address2, null, request.getRemoteAddr(), "n");
			try {
				int result = dao.insertMember(dto);
				request.setAttribute("result", result);
				request.getRequestDispatcher("/WEB-INF/basics/alertJoin.jsp").forward(request, response);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}else if(cmd.contentEquals("/LoginForm.members")) {
			request.getRequestDispatcher("/WEB-INF/basics/loginForm.jsp").forward(request, response);
		} else if (cmd.equals("/Login.members")) {
			String email = request.getParameter("email");
			String pw = request.getParameter("pw");
			try {
				boolean result = dao.isLoginOk(email, pw);
				if (result) {
					request.getSession().setAttribute("loginEmail", email);
				}
				request.setAttribute("result", result);
				request.getRequestDispatcher("/WEB-INF/basics/alertLogin.jsp").forward(request, response);
			} catch (Exception e) {
				e.printStackTrace();
			}

		} else if (cmd.equals("/Logout.members")) {
			request.getSession().invalidate();
			request.getRequestDispatcher("/WEB-INF/basics/alertLogout.jsp").forward(request, response);

		} else if (cmd.equals("/naverLogin.members")) {
			String clientId = "9fcJ6ehu7V7mEFnBQABz";// 애플리케이션 클라이언트 아이디값";
			String clientSecret = "otERPitybs";// 애플리케이션 클라이언트 시크릿값";
			String code = request.getParameter("code");
			String state = request.getParameter("state");
			String redirectURI = URLEncoder.encode("http://localhost/naverLogin.members", "UTF-8");
			String apiURL;
			apiURL = "https://nid.naver.com/oauth2.0/token?grant_type=authorization_code&";
			apiURL += "client_id=" + clientId;
			apiURL += "&client_secret=" + clientSecret;
			apiURL += "&redirect_uri=" + redirectURI;
			apiURL += "&code=" + code;
			apiURL += "&state=" + state;
			String access_token = "";
			String refresh_token = "";
			try {
				URL url = new URL(apiURL);
				HttpURLConnection con = (HttpURLConnection) url.openConnection();
				con.setRequestMethod("GET");
				int responseCode = con.getResponseCode();
				BufferedReader br;
				System.out.print("responseCode=" + responseCode);
				if (responseCode == 200) { // 정상 호출
					br = new BufferedReader(new InputStreamReader(con.getInputStream()));
				} else { // 에러 발생
					br = new BufferedReader(new InputStreamReader(con.getErrorStream()));
				}
				String inputLine;
				StringBuffer res = new StringBuffer();
				while ((inputLine = br.readLine()) != null) {
					res.append(inputLine);
				}
				br.close();
				if (responseCode == 200) {
					String ip = request.getRemoteAddr();
					MemberDTO dto = dao.NaverContentsParse(res.toString(), ip);
					if (dao.isIdExist(dto)) {
						request.getSession().setAttribute("loginEmail", dto.getEmail());
						request.getRequestDispatcher("main.jsp").forward(request, response);
					} else {
						dao.insertNaverMember(dto);
						request.getSession().setAttribute("loginEmail", dto.getEmail());
						request.getRequestDispatcher("main.jsp").forward(request, response);
					}
				}
			} catch (Exception e) {
				System.out.println(e);
				e.printStackTrace();
				response.sendRedirect("error.html");
			}

		}else if(cmd.equals("/Mypage.members")) {
			//String email = (String) request.getSession().getAttribute("loginEmail");
			String email = "email5@email.mail";
			int currentPage = Integer.parseInt(request.getParameter("currentPage"));
			System.out.println(currentPage);
				
			try {	
				//내가 후원한 글 목록---------------------------------------------------------------
				int endNumforMS = currentPage * 5;
				int startNumforMS = endNumforMS - 4;
				request.setAttribute("mySupport", dao.mySupport(email, startNumforMS, endNumforMS));
				/*페이지*/
				request.setAttribute("getNaviforMS", dao.getNaviforMySupport(currentPage));
				//내가 쓴 글 목록------------------------------------------------------------------
				int endNum = currentPage * 5;
				int startNum = endNum - 4;
				request.setAttribute("myArticles",dao.myArticles(email, startNum, endNum));
				/*페이지*/	
				String getNavi = dao.getNavi(currentPage);
				request.setAttribute("getNavi", getNavi);
				//---------------------------------------------------------------------------
				request.getRequestDispatcher("/WEB-INF/basics/myPage.jsp").forward(request, response);
			}catch (Exception e) {
				e.printStackTrace();
				response.sendRedirect("error.html");
			}
		}

	
	}
	protected void doPost(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		doGet(request, response);
	}

}