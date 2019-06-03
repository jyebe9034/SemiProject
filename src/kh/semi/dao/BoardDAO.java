package kh.semi.dao;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import kh.semi.dto.BoardDTO;
import kh.semi.dto.BoardListDTO;
import kh.semi.dto.TitleImgDTO;
import kh.semi.dto.UfileDTO;


import kh.semi.dto.CommentDTO;
import kh.semi.dto.TitleImgDTO;

public class BoardDAO {
	static int recordCountPerPage = 8;
	static int boardRecordCountPerPage = 10;
	static int naviCountPerPage = 5;
	public static int pageTotalCount;

	public Connection getConnection() throws Exception {
		Class.forName("oracle.jdbc.driver.OracleDriver");
		String url = "jdbc:oracle:thin:@localhost:1521:xe";
		String user = "semi";
		String pw = "semi";
		return DriverManager.getConnection(url,user,pw);
	}

	public PreparedStatement pstatForGetDataForMain(Connection con)throws Exception{
		String sql = "select b_due_date-sysdate as d_day, b_no, b_title, b_amount, "
				+ "b_due_date, b_sum_amount from board where b_due_date-sysdate like'+%' order by d_day";
		PreparedStatement pstat = con.prepareStatement(sql);
		return pstat;
	}
	
	public List<BoardDTO> getDataForMain() throws Exception{
		try(
				Connection con = this.getConnection();
				PreparedStatement pstat = this.pstatForGetDataForMain(con);
				ResultSet rs = pstat.executeQuery();
				){
			List<BoardDTO> list = new ArrayList<>();
			for(int i=0; i<3;i++) {
				if(rs.next()) {
					BoardDTO dto = new BoardDTO();
					dto.setBoardNo(rs.getInt("b_no"));
					dto.setTitle(rs.getString("b_title"));
					dto.setAmount(rs.getInt("b_amount"));
					dto.setDueDate(rs.getTimestamp("b_due_date"));
					dto.setSumAmount(rs.getInt("b_sum_amount"));
					list.add(dto);
				}
			}
			return list;
		}
	}
	public int insertBoard(BoardDTO dto)throws Exception{
		String sql = "insert into Board values(b_no_seq.nextval,?,?,?,?,?,?,?,?,default,default,default,default)";
		try(
				Connection con = this.getConnection();
				PreparedStatement pstat = con.prepareStatement(sql);
				){
			pstat.setString(1,dto.getTitle());
			pstat.setString(2, dto.getEmail());
			pstat.setString(3,dto.getWriter());
			pstat.setInt(4,dto.getAmount());
			pstat.setString(5,dto.getBank());
			pstat.setString(6,dto.getAccount());
			pstat.setTimestamp(7, dto.getDueDate());
			pstat.setString(8, dto.getContents());
			int result = pstat.executeUpdate();
			con.commit();
			return result;
		}

	}

	private PreparedStatement pstatForSelectOneArticle(Connection con, int boardNo)throws Exception{
		String sql = "select * from board where b_no=?";
		PreparedStatement pstat = con.prepareStatement(sql);
		pstat.setInt(1, boardNo);
		return pstat;
	}

	public BoardDTO selectOneArticle(int boardNo)throws Exception{
		try(
				Connection con = this.getConnection();
				PreparedStatement pstat = pstatForSelectOneArticle(con, boardNo);
				ResultSet rs = pstat.executeQuery();
				){
			BoardDTO boardDTO = new BoardDTO();
			if(rs.next()) {
				boardDTO.setBoardNo(rs.getInt("b_no"));
				boardDTO.setTitle(rs.getString("b_title"));
				boardDTO.setEmail(rs.getString("b_email"));
				boardDTO.setWriter(rs.getString("b_writer"));
				boardDTO.setAmount(rs.getInt("b_amount"));
				boardDTO.setBank(rs.getString("b_bank"));
				boardDTO.setAccount(rs.getString("b_account"));
				boardDTO.setContents(rs.getString("b_contents"));
				boardDTO.setDueDate(rs.getTimestamp("b_due_date"));
				boardDTO.setViewCount(rs.getInt("b_viewcount"));
				boardDTO.setWriteDate(rs.getTimestamp("b_writedate"));
				boardDTO.setRecommend(rs.getInt("b_recommend"));
				boardDTO.setSumAmount(rs.getInt("b_sum_amount"));
				return boardDTO;
			}
			return null;
		}
	}

//*게시판*//=================================================================================================
	//totalCount//
	/*(1)전체 게시글의 개수*/
	public int totalRecordNum() throws Exception{
	//	String sql = "select * from board order by b_no desc";
		String sql = "select row_number() over(order by b_no desc) as rown, t1.*,t2.* from board t1 join title_img t2 on (t1.b_no = t2.t_b_no)";
		try(
				Connection con = this.getConnection();
				PreparedStatement ps = con.prepareStatement(sql);
				ResultSet rs = ps.executeQuery();	
				){
			int result = 0;
			while(rs.next()) {
				result++;
			}
			return result;
		}
	}
	/*(2)searchOption으로 검색했을 때 전체 게시글 개수*/
	public PreparedStatement psForSearchOption(Connection con, String searchOption, String searchWord) throws Exception{
		//String sql = "select * from board where " + searchOption + " like ? order by b_no desc";
		String sql = "select * from board t1 join title_img t2 on (t1.b_no = t2.t_b_no) where "+searchOption+" like ? order by b_no desc";
		PreparedStatement ps = con.prepareStatement(sql);
		ps.setString(1, "%"+searchWord+"%");
		return ps;
	}
	public int totalRecordNumBySearch(String searchOption, String searchWord) throws Exception{
		try(
				Connection con = this.getConnection();
				PreparedStatement ps = this.psForSearchOption(con, searchOption, searchWord);
				ResultSet rs = ps.executeQuery();	
				){
			int result = 0;
			while(rs.next()) {
				result++;
			}
			return result;
		}
	}
	
	/*(1)전체 게시판 목록*/
	public PreparedStatement psForSelectByPage(Connection con, int currentPage) throws Exception{
		String sql = "select row_number() over(order by b_no desc) as rown, t1.*,t2.* from board t1 join title_img t2 on (t1.b_no = t2.t_b_no)";
		PreparedStatement ps = con.prepareStatement(sql);
		return ps;
	}
	public List<BoardListDTO> selectByPage(int currentPage) throws Exception{

		try(
				Connection con = this.getConnection();
				PreparedStatement ps = this.psForSelectByPage(con, currentPage);
				ResultSet rs = ps.executeQuery();	
				){
			List<BoardListDTO> result = new ArrayList<>();
			while(rs.next()) {

				int boardNo = rs.getInt("b_no");
				String email = rs.getString("b_email");
				String title = rs.getString("b_title");
				String writer = rs.getString("b_writer");
				int amount = rs.getInt("b_amount");
				String bank = rs.getString("b_bank");
				String account = rs.getString("b_account");
				String dueDate = rs.getString("b_due_date");
				String contents = rs.getString("b_contents1")+rs.getString("b_contents2")+rs.getString("b_contents3");
				int viewCount = rs.getInt("b_viewcount");
				String writeDate = rs.getString("b_writedate");
				int recommend = rs.getInt("b_recommend");
				int sumAmount = rs.getInt("b_sum_amount");
				
				String fileName = rs.getString("t_fileName");
				String oriFileName = rs.getString("t_oriFileName");					
				String filePath = rs.getString("t_filePath");
				long fileSize = rs.getLong("t_fileSize");				

				BoardListDTO dto = new BoardListDTO(boardNo,email,title,writer,amount,bank,account,dueDate,contents,viewCount,writeDate,recommend,sumAmount,
						fileName,oriFileName,filePath,fileSize);
				result.add(dto);
			}
			return result;
		}
	}	

	
	/*검색*///검색어=searchWord
	/*(2)searchOption으로 검색했을 때 게시글 목록*/
	public PreparedStatement psForSearchList(Connection con, String searchOption, String searchWord, int currentPage) throws Exception{
		String sql = "select * from board t1 join title_img t2 on (t1.b_no = t2.t_b_no) where "+searchOption+" like ? order by b_no desc";

		PreparedStatement ps = con.prepareStatement(sql);
		ps.setString(1, "%"+searchWord+"%");
		return ps;
	}
	
	public int insertTitleImg(TitleImgDTO dto) throws Exception {
		String sql = "insert into title_img values(?, ?, ?, ?, ?)";
		try(
				Connection con = this.getConnection();
				PreparedStatement pstat = con.prepareStatement(sql);
				){
			pstat.setInt(1, dto.getBoardNo());
			pstat.setString(2, dto.getFileName());
			pstat.setString(3, dto.getOriFileName());
			pstat.setString(4, dto.getFilePath());
			pstat.setLong(5, dto.getFileSize());
			int result = pstat.executeUpdate();
			con.commit();
			return result;
		}
	}

	private PreparedStatement pstatForGetTitleImg(Connection con, int boardNo) throws Exception {
		String sql = "select * from title_img where t_b_no=?";
		PreparedStatement pstat = con.prepareStatement(sql);
		pstat.setInt(1, boardNo);
		return pstat;
	}

	public int updateSumAccount(int amount, int boardNo) throws Exception {
		String sql = "update board set b_sum_amount=b_sum_amount+? where b_No=?";
		try(
				Connection con = this.getConnection();
				PreparedStatement pstat = con.prepareStatement(sql);
				){
			pstat.setInt(1, amount);
			pstat.setInt(2, boardNo);
			int result = pstat.executeUpdate();
			con.commit();
			return result;
		}
	}

	private PreparedStatement pstatForRecommendCheck(Connection con, String email, int boardNo) throws Exception {
		String sql = "select * from recommend where r_email=? and r_b_no=?";
		PreparedStatement pstat = con.prepareStatement(sql);
		pstat.setString(1, email);
		pstat.setInt(2, boardNo);
		return pstat;
	}
	public boolean recommendCheck(String email, int boardNo) throws Exception {
		String sql = "select * from recommend where r_email=? and r_b_no=?";
		try(
				Connection con = this.getConnection();
				PreparedStatement pstat = this.pstatForRecommendCheck(con, email, boardNo);
				ResultSet rs = pstat.executeQuery();
				){
			if(rs.next()) {
				return false;
			}
			return true;
		}
	}

	public boolean updateRecommend(String email, int boardNo, String title) throws Exception{
		String sql1 = "update board set b_recommend=b_recommend+1 where b_no=?";
		String sql2 = "insert into recommend values(?, ?, ?)";
		try(
				Connection con = this.getConnection();
				PreparedStatement pstat1 = con.prepareStatement(sql1);
				PreparedStatement pstat2 = con.prepareStatement(sql2);
				){
			pstat1.setInt(1, boardNo);

			pstat2.setString(1, email);
			pstat2.setInt(2, boardNo);
			pstat2.setString(3, title);

			int result = pstat1.executeUpdate();
			int result2 = pstat2.executeUpdate();
			con.commit();

			if(result>0 && result2>0) {
				return true;
			}
			return false;
		}
	}

	public int insertComment(String email, String name, int boardNo, String comment) throws Exception {
		String sql = "insert into comments values(?, ?, ?, ?, default)";
		try(
				Connection con = this.getConnection();
				PreparedStatement pstat = con.prepareStatement(sql);
				){
			pstat.setString(1, email);
			pstat.setString(2, name);
			pstat.setInt(3, boardNo);
			pstat.setString(4, comment);
			int result = pstat.executeUpdate();
			con.commit();
			return result;
		}
	}

	public List<CommentDTO> selectCommentsByBoardNo(int commentPage, int boardNo) throws Exception {
		String sql = "select * from comments where c_b_no=" + boardNo + " order by c_write_date desc";
		try(
				Connection con = this.getConnection();
				PreparedStatement pstat = con.prepareStatement(sql);
				ResultSet rs = pstat.executeQuery();
				){
			List<CommentDTO> result = new ArrayList<>();
			while(rs.next()) {
				result.add(new CommentDTO(rs.getString(1), rs.getString(2), rs.getInt(3), rs.getString(4), rs.getTimestamp(5)));
			}

			int fromIndex = (commentPage*recordCountPerPage)-(recordCountPerPage-1);
			int toIndex = commentPage*recordCountPerPage;

			if(toIndex > result.size()) {
				toIndex = result.size();
			}

			return result.subList(fromIndex-1, toIndex);
		}
	}

	public int deleteComment(String email, String writeDate) throws Exception {
		String sql = "delete comments where c_email=? and c_write_date=?";
		try(
				Connection con = this.getConnection();
				PreparedStatement pstat = con.prepareStatement(sql);
				){
			pstat.setString(1, email);
			pstat.setTimestamp(2, Timestamp.valueOf(writeDate));
			int result = pstat.executeUpdate();
			con.commit();
			return result;
		}
	}

	public Map<String, Integer> getCommentNavi(int currentPage, int recordTotalCount) {
		// 가지고 있는 게시글의 수에 맞는 페이지의 개수를 구함.
		int pageTotalCount = recordTotalCount / recordCountPerPage;
		if(recordTotalCount % recordCountPerPage > 0) {
			pageTotalCount++;
		}
		// 현재 페이지 오류 검출 및 정정
		if(currentPage < 1) {
			currentPage = 1;
		}
		else if(currentPage > pageTotalCount) {
			currentPage = pageTotalCount;
		}
		// 네비게이터 시작과 끝
		int startNavi = ((currentPage-1)/naviCountPerPage)*naviCountPerPage + 1;
		int endNavi = startNavi + (naviCountPerPage - 1);
		// 네비 끝값이 최대 페이지 번호를 넘어가면 최대 페이지번호로 네비 끝값을 설정한다.
		if(endNavi > pageTotalCount) {
			endNavi = pageTotalCount;
		}
		int needPrev = 1;	// 1이면 true, -1이면 false
		int needNext = 1;

		if(startNavi == 1) {
			needPrev = -1;
		}
		if(endNavi == pageTotalCount) {
			needNext = -1;
		}

		Map<String, Integer> pageNavi = new HashMap<>();
		pageNavi.put("currentPage", currentPage);
		pageNavi.put("recordTotalCount", recordTotalCount);
		pageNavi.put("recordCountPerPage", recordCountPerPage);
		pageNavi.put("naviCountPerPage", naviCountPerPage);
		pageNavi.put("pageTotalCount", pageTotalCount);
		pageNavi.put("startNavi", startNavi);
		pageNavi.put("endNavi", endNavi);
		pageNavi.put("needPrev", needPrev);
		pageNavi.put("needNext", needNext);

		return pageNavi;
	}
	public List<BoardListDTO> searchList(String searchOption, String searchWord, int currentPage) throws Exception{
		try(
				Connection con = this.getConnection();
				PreparedStatement ps = this.psForSearchList(con, searchOption, searchWord, currentPage);
				ResultSet rs = ps.executeQuery();
				){
			List<BoardListDTO> result = new ArrayList<>();
			while(rs.next()) {
				int boardNo = rs.getInt("b_no");
				String email = rs.getString("b_email");
				String title = rs.getString("b_title");
				String writer = rs.getString("b_writer");
				int amount = rs.getInt("b_amount");
				String bank = rs.getString("b_bank");
				String account = rs.getString("b_account");
				String dueDate = rs.getString("b_due_date");
				String contents = rs.getString("b_contents1")+rs.getString("b_contents2")+rs.getString("b_contents3");
				int viewCount = rs.getInt("b_viewcount");
				String writeDate = rs.getString("b_writedate");
				int recommend = rs.getInt("b_recommend");
				int sumAmount = rs.getInt("b_sum_amount");
				String fileName = rs.getString("t_fileName");
				String oriFileName = rs.getString("t_oriFileName");					
				String filePath = rs.getString("t_filePath");
				long fileSize = rs.getLong("t_fileSize");				

				BoardListDTO dto = new BoardListDTO(boardNo,email,title,writer,amount,bank,account,dueDate,contents,viewCount,writeDate,recommend,sumAmount,
						fileName,oriFileName,filePath,fileSize);
				result.add(dto);
			}
			return result;
		}	
	}
	
	/*페이지 네비게이터*/
	public Map<String, Integer> getNavi(int currentPage, int boardRecordTotalCount) {
	      // 가지고 있는 게시글의 수에 맞는 페이지의 개수를 구함.
	      int pageTotalCount = boardRecordTotalCount / recordCountPerPage;
	      if(boardRecordTotalCount % recordCountPerPage > 0) {
	         pageTotalCount++;
	      }
	      // 현재 페이지 오류 검출 및 정정
	      if(currentPage < 1) {
	         currentPage = 1;
	      }
	      else if(currentPage > pageTotalCount) {
	         currentPage = pageTotalCount;
	      }
	      // 네비게이터 시작과 끝
	      int startNavi = ((currentPage-1)/naviCountPerPage)*naviCountPerPage + 1;
	      int endNavi = startNavi + (naviCountPerPage - 1);
	      // 네비 끝값이 최대 페이지 번호를 넘어가면 최대 페이지번호로 네비 끝값을 설정한다.
	      if(endNavi > pageTotalCount) {
	         endNavi = pageTotalCount;
	      }
	      int needPrev = 1;   // 1이면 true, -1이면 false
	      int needNext = 1;

	      if(startNavi == 1) {
	         needPrev = -1;
	      }
	      if(endNavi == pageTotalCount) {
	         needNext = -1;
	      }

	      Map<String, Integer> pageNavi = new HashMap<>();
	      pageNavi.put("currentPage", currentPage);
	      pageNavi.put("recordTotalCount", boardRecordTotalCount);
	      pageNavi.put("recordCountPerPage", recordCountPerPage);
	      pageNavi.put("naviCountPerPage", naviCountPerPage);
	      pageNavi.put("pageTotalCount", pageTotalCount);
	      pageNavi.put("startNavi", startNavi);
	      pageNavi.put("endNavi", endNavi);
	      pageNavi.put("needPrev", needPrev);
	      pageNavi.put("needNext", needNext);

	      return pageNavi;
	   }

	/*(2)내용으로 검색*/ //searchContents
//		public PreparedStatement psForSearchContents(Connection con, String searchWord, int startNum, int endNum) throws Exception{
//			String sql = "select * from (select row_number() over(order by b_no desc) as rown, board.* from board where B_contents1 || b_contents2 ||b_contents3 like '%?%') where rown between ? and ?";
//			PreparedStatement ps = con.prepareStatement(sql);
//			ps.setString(1, searchWord);
//			ps.setInt(2, startNum);
//			ps.setInt(3, endNum);
//			return ps;
//		}
//		public List<BoardListDTO> searchContents(String searchWord, int startNum, int endNum) throws Exception{
//			try(
//					Connection con = this.getConnection();
//					PreparedStatement ps = this.psForSearchContents(con, searchWord, startNum, endNum);
//					ResultSet rs = ps.executeQuery();
//					){
//				List<BoardListDTO> result = new ArrayList<>();
//				while(rs.next()) {
//					int boardNo = rs.getInt("b_no");
//					String email = rs.getString("b_email");
//					String title = rs.getString("b_title");
//					String writer = rs.getString("b_writer");
//					int amount = rs.getInt("b_amount");
//					String bank = rs.getString("b_bank");
//					String account = rs.getString("b_account");
//					String dueDate = rs.getString("b_due_date");
//					String contents = rs.getString("b_contents1")+rs.getString("b_contents2")+rs.getString("b_contents3");
//					int viewCount = rs.getInt("b_viewcount");
//					String writeDate = rs.getString("b_writedate");
//					int recommend = rs.getInt("b_recommend");
//					int sumAmount = rs.getInt("b_sum_amount");
//					int fileSeq = rs.getInt("t_fileSeq");
//					String fileName = rs.getString("t_fileName");
//					String oriFileName = rs.getString("t_oriFileName");					
//					String filePath = rs.getString("t_filePath");
//					long fileSize = rs.getLong("t_fileSize");				
//
//					BoardListDTO dto = new BoardListDTO(boardNo,email,title,writer,amount,bank,account,dueDate,contents,viewCount,writeDate,recommend,sumAmount,fileSeq,
//							fileName,oriFileName,filePath,fileSize);
//				}
//				return result;
//			}		
//		}	
	/*(3)제목+내용으로 검색*/ //searchAll
//		public PreparedStatement psForSearchAll(Connection con, String searchOption, String searchWord, int startNum, int endNum) throws Exception{
//			String sql = "select * from (select row_number() over(order by b_no desc) as rown, board.* from board where "+searchOption+" like '%?%') where rown between ? and ?";
//			PreparedStatement ps = con.prepareStatement(sql);
//			ps.setString(1, searchWord);
//			ps.setInt(2, startNum);
//			ps.setInt(3, endNum);
//			return ps;
//		}
//		public List<BoardListDTO> searchAll(String searchWord, int startNum, int endNum) throws Exception{
//			try(
//					Connection con = this.getConnection();
//					PreparedStatement ps = this.psForSearchContents(con, searchWord, startNum, endNum);
//					ResultSet rs = ps.executeQuery();
//					){
//				List<BoardListDTO> result = new ArrayList<>();
//				while(rs.next()) {
//					int boardNo = rs.getInt("b_no");
//					String email = rs.getString("b_email");
//					String title = rs.getString("b_title");
//					String writer = rs.getString("b_writer");
//					int amount = rs.getInt("b_amount");
//					String bank = rs.getString("b_bank");
//					String account = rs.getString("b_account");
//					String dueDate = rs.getString("b_due_date");
//					String contents = rs.getString("b_contents1")+rs.getString("b_contents2")+rs.getString("b_contents3");
//					int viewCount = rs.getInt("b_viewcount");
//					String writeDate = rs.getString("b_writedate");
//					int recommend = rs.getInt("b_recommend");
//					int sumAmount = rs.getInt("b_sum_amount");
//					int fileSeq = rs.getInt("t_fileSeq");
//					String fileName = rs.getString("t_fileName");
//					String oriFileName = rs.getString("t_oriFileName");					
//					String filePath = rs.getString("t_filePath");
//					long fileSize = rs.getLong("t_fileSize");				
//
//					BoardListDTO dto = new BoardListDTO(boardNo,email,title,writer,amount,bank,account,dueDate,contents,viewCount,writeDate,recommend,sumAmount,fileSeq,
//							fileName,oriFileName,filePath,fileSize);
//				}
//				return result;
//			}		
//		}	

	
}
