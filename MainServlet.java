package cn.servlet;

import cn.entity.*;
import cn.listen.UserListener;
import cn.servers.*;
import cn.util.Page;
import cn.util.StringUtil;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONWriter;
import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;
import java.io.*;
import java.lang.reflect.Method;
import java.util.List;
import java.util.UUID;

/**
 * Author : atyushu
 * Package : cn. servlet
 * ClassName : MainServlet
 * Create : 2023/6/13 - 17:20
 * Version : v1.0
 * Description : The class of servlet for all school database.
 */
@WebServlet(name = "MainServlet", value = "*.do")
public class MainServlet extends HttpServlet {

    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        this.doPost(request, response);
    }

    @Override
    public void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        request.setCharacterEncoding("UTF-8");
        response.setContentType("text/html;charset=UTF-8");
        String servletPath = request.getServletPath();
        servletPath = servletPath.substring(1, servletPath.length() - 3);
        try {
            Method method = this.getClass().getMethod(servletPath, HttpServletResponse.class, HttpServletRequest.class);
            Object invoke = method.invoke(this, response, request);
            if (invoke == null)
                return;
            if (invoke instanceof String) {
                request.getRequestDispatcher((String) invoke).forward(request, response);
                return;
            }

            String objectJsonString = JSON.toJSONString(invoke, "yyyy-MM-dd", JSONWriter.Feature.NullAsDefaultValue);
            response.setContentType("application/json;charset=utf-8");
            response.getWriter().print(objectJsonString);
        } catch (NoSuchMethodException noSuchMethodException) {
            noSuchMethodException.printStackTrace();
            response.getWriter().print("> request method not found : " + servletPath);
        } catch (Exception exception) {
            exception.printStackTrace();
            response.getWriter().print("> request method error");
        }
    }

    public void studentPage(HttpServletResponse response, HttpServletRequest request) throws ServletException, IOException {
        int pageNo = StringUtil.intParse(request.getParameter("pageNo"), 1);
        pageNo = pageNo < 1 ? 1 : pageNo;
        String pageGo = request.getParameter("pageGo");
        if (pageGo != null && pageGo != "") {
            pageNo = Integer.parseInt(pageGo);
        }
        String sname = StringUtil.nullToBlank(request.getParameter("sname"));
        String tel = StringUtil.nullToBlank(request.getParameter("tel"));
        String qq = StringUtil.nullToBlank(request.getParameter("qq"));
        String start = StringUtil.nullToBlank(request.getParameter("start"));
        String end = StringUtil.nullToBlank(request.getParameter("end"));
        Page<Student> studentPage = StudentServer.SELF.pageByLike(pageNo, Page.PAGE_SIZE, sname, tel, qq, StringUtil.dateParse(start), StringUtil.dateParse(end));
        List<Grade> grades = GradeServer.SELF.getAllInfo();
        request.setAttribute("grades", grades);
        request.setAttribute("sname", sname);
        request.setAttribute("tel", tel);
        request.setAttribute("qq", qq);
        request.setAttribute("start", start);
        request.setAttribute("end", end);
        request.setAttribute("useronline", UserListener.userOnline);
        request.setAttribute("studentPage", studentPage);
        request.getRequestDispatcher("/WEB-INF/student.jsp").forward(request, response);
        System.out.println("会是乱码嘛");
    }

    public void studentDelete(HttpServletResponse response, HttpServletRequest request) throws ServletException, IOException {
        int delete = StudentServer.SELF.delete(Integer.parseInt(request.getParameter("deleteStudentNo")));
        String msg = delete >= 1 ? "删除成功" : "删除失败";
        response.getWriter().println("<style>*{font-size:32px; text-align:center;color:orange;}</style> <script>document.write('" + msg + "'); window.setTimeout(()=>{window.location='studentPage.do';},1000);</script>");
    }

    public void studentAdd(HttpServletResponse response, HttpServletRequest request) throws ServletException, IOException {
        Student student = new Student();
        DiskFileItemFactory df = new DiskFileItemFactory();
        ServletFileUpload upload = new ServletFileUpload(df);
        upload.setHeaderEncoding("utf-8");
        List<FileItem> fileItems = null;
        try {
            fileItems = upload.parseRequest(request);
        } catch (FileUploadException e) {
        }
        for (FileItem fi : fileItems) {
            if (fi.isFormField()) {
                if (fi.getFieldName().equals("sname")) {
                    student.setSname(fi.getString("utf-8"));
                } else if (fi.getFieldName().equals("sex")) {
                    student.setSex(fi.getString("utf-8"));
                } else if (fi.getFieldName().equals("telephone")) {
                    student.setTelephone(fi.getString("utf-8"));
                } else if (fi.getFieldName().equals("qq")) {
                    student.setQq(fi.getString("utf-8"));
                } else if (fi.getFieldName().equals("wechat")) {
                    student.setWechat(fi.getString("utf-8"));
                } else if (fi.getFieldName().equals("gradeid")) {
                    student.setGradeid(Integer.parseInt(fi.getString("utf-8")));
                } else if (fi.getFieldName().equals("enterdate")) {
                    student.setEnterdate(StringUtil.dateParse(fi.getString("utf-8")));
                } else if (fi.getFieldName().equals("birthday")) {
                    student.setBirthday(StringUtil.dateParse(fi.getString("utf-8")));
                } else if (fi.getFieldName().equals("fromcity")) {
                    student.setFromcity(fi.getString("utf-8"));
                } else if (fi.getFieldName().equals("address")) {
                    student.setAddress(fi.getString("utf-8"));
                } else if (fi.getFieldName().equals("pass")) {
                    student.setPass(fi.getString("utf-8"));
                }
                continue;
            }
            if (fi.getSize() < 1) {
                continue;
            }
            String oldName = fi.getName();
            String houzhui = oldName.substring(oldName.lastIndexOf('.'));
            String newName = UUID.randomUUID() + houzhui;
            ServletContext application = super.getServletContext();
            String path0 = application.getRealPath("img");
            File path = new File(path0);
            if (!path.exists())
                path.mkdirs();
            try {
                fi.write(new File(path, newName));
            } catch (Exception e) {

            }
            student.setIdcardpic("img/" + newName);
        }
        int insert = StudentServer.SELF.insert(student);
        String msg = insert >= 1 ? "添加成功" : "添加失败";
        response.getWriter().println("<style>*{font-size:32px; text-align:center;color:orange;}</style> <script>document.write('" + msg + "'); window.setTimeout(()=>{window.location='studentPage.do';},1000);</script>");
    }

    public void studentChange(HttpServletResponse response, HttpServletRequest request) throws ServletException, IOException {
        Student student = new Student();
        DiskFileItemFactory df = new DiskFileItemFactory();
        ServletFileUpload upload = new ServletFileUpload(df);
        upload.setHeaderEncoding("utf-8");
        List<FileItem> fileItems = null;
        try {
            fileItems = upload.parseRequest(request);
        } catch (FileUploadException e) {

        }
        for (FileItem fi : fileItems) {
            if (fi.isFormField()) {
                if (fi.getFieldName().equals("sname")) {
                    student.setSname(fi.getString("utf-8"));
                } else if (fi.getFieldName().equals("stuno")) {
                    student.setStuno(Integer.parseInt(fi.getString("utf-8")));
                } else if (fi.getFieldName().equals("sex")) {
                    student.setSex(fi.getString("utf-8"));
                } else if (fi.getFieldName().equals("telephone")) {
                    student.setTelephone(fi.getString("utf-8"));
                } else if (fi.getFieldName().equals("qq")) {
                    student.setQq(fi.getString("utf-8"));
                } else if (fi.getFieldName().equals("wechat")) {
                    student.setWechat(fi.getString("utf-8"));
                } else if (fi.getFieldName().equals("gradeid")) {
                    student.setGradeid(Integer.parseInt(fi.getString("utf-8")));
                } else if (fi.getFieldName().equals("enterdate")) {
                    student.setEnterdate(StringUtil.dateParse(fi.getString("utf-8")));
                } else if (fi.getFieldName().equals("birthday")) {
                    student.setBirthday(StringUtil.dateParse(fi.getString("utf-8")));
                } else if (fi.getFieldName().equals("fromcity")) {
                    student.setFromcity(fi.getString("utf-8"));
                } else if (fi.getFieldName().equals("address")) {
                    student.setAddress(fi.getString("utf-8"));
                } else if (fi.getFieldName().equals("pass")) {
                    student.setPass(fi.getString("utf-8"));
                }
                continue;
            }
            if (fi.getSize() < 1) {
                continue;
            }
            String oldName = fi.getName();
            String houzhui = oldName.substring(oldName.lastIndexOf('.'));
            String newName = UUID.randomUUID() + houzhui;
            ServletContext application = super.getServletContext();
            String path0 = application.getRealPath("img");
            File path = new File(path0);
            if (!path.exists())
                path.mkdirs();
            try {
                fi.write(new File(path, newName));
            } catch (Exception e) {

            }
            student.setIdcardpic("img/" + newName);
        }
        int update = StudentServer.SELF.update(student);
        String msg = update >= 1 ? "修改成功" : "修改失败";
        response.getWriter().println("<style>*{font-size:32px; text-align:center;color:orange;}</style> <script>document.write('" + msg + "'); window.setTimeout(()=>{window.location='studentPage.do';},1000);</script>");
    }

    public void subjectPage(HttpServletResponse response, HttpServletRequest request) throws ServletException, IOException {
        int pageNo = StringUtil.intParse(request.getParameter("pageNo"), 1);
        String pageGo = request.getParameter("pageGo");
        if (pageGo != null && pageGo != "") {
            pageNo = Integer.parseInt(pageGo);
        }
        String subjectname = StringUtil.nullToBlank(request.getParameter("subjectname"));
        Page<Subject> subjectPage = SubjectServer.SELF.pageByLike(pageNo, Page.PAGE_SIZE, subjectname);
        List<Grade> grades = GradeServer.SELF.getAllInfo();
        request.setAttribute("grades", grades);
        request.setAttribute("subjectPage", subjectPage);
        request.setAttribute("subjectname", subjectname);
        request.setAttribute("useronline", UserListener.userOnline);
        request.getRequestDispatcher("/WEB-INF/subject.jsp").forward(request, response);
    }

    public void subjectDelete(HttpServletResponse response, HttpServletRequest request) throws ServletException, IOException {
        int delete = SubjectServer.SELF.delete(Integer.parseInt(request.getParameter("subjectIdDelete")));
        String msg = delete >= 1 ? "删除成功" : "删除失败";
        response.getWriter().println("<style>*{font-size:32px; text-align:center;color:orange;}</style> <script>document.write('" + msg + "'); window.setTimeout(()=>{window.location='subjectPage.do';},1000);</script>");
    }

    public void subjectChange(HttpServletResponse response, HttpServletRequest request) throws ServletException, IOException {
        response.getWriter().println("<style>*{font-size:32px; text-align:center;color:orange;}</style> <script>document.write('" + (SubjectServer.SELF.update(new Subject(Integer.parseInt(request.getParameter("subjectId")), request.getParameter("subjectName"), Integer.parseInt(request.getParameter("classHour")), Integer.parseInt(request.getParameter("gradeId")))) >= 1 ? "修改成功" : "修改失败") + "'); window.setTimeout(()=>{window.location='subjectPage.do';},1000);</script>");
    }

    public void subjectAdd(HttpServletResponse response, HttpServletRequest request) throws ServletException, IOException {
        response.getWriter().println("<style>*{font-size:32px; text-align:center;color:orange;}</style> <script>document.write('" + (SubjectServer.SELF.insert(new Subject(request.getParameter("subjectName"), Integer.parseInt(request.getParameter("classHour")), Integer.parseInt(request.getParameter("gradeId")))) >= 1 ? "添加成功" : "添加失败") + "'); window.setTimeout(()=>{window.location='subjectPage.do';},1000);</script>");
    }

    public void gradePage(HttpServletResponse response, HttpServletRequest request) throws ServletException, IOException {
        int pageNo = StringUtil.intParse(request.getParameter("pageNo"), 1);
        String pageGo = request.getParameter("pageGo");
        if (pageGo != null && pageGo != "") {
            pageNo = Integer.parseInt(pageGo);
        }
        String gname = StringUtil.nullToBlank(request.getParameter("gname"));
        Page<Grade> gradePage = GradeServer.SELF.pageByLike(pageNo, Page.PAGE_SIZE, gname);
        request.setAttribute("gname", gname);
        request.setAttribute("useronline", UserListener.userOnline);
        request.setAttribute("gradePage", gradePage);
        request.getRequestDispatcher("/WEB-INF/grade.jsp").forward(request, response);
    }

    public void gradeAdd(HttpServletResponse response, HttpServletRequest request) throws ServletException, IOException {
        response.getWriter().println("<style>*{font-size:32px; text-align:center;color:orange;}</style> <script>document.write('" + (GradeServer.SELF.insert(new Grade(request.getParameter("gname"))) >= 1 ? "添加成功" : "添加失败") + "'); window.setTimeout(()=>{window.location='gradePage.do';},1000);</script>");
    }

    public void gradeDelete(HttpServletResponse response, HttpServletRequest request) throws ServletException, IOException {
        response.getWriter().println("<style>*{font-size:32px; text-align:center;color:orange;}</style> <script>document.write('" + (GradeServer.SELF.delete(Integer.parseInt(request.getParameter("gradeId"))) >= 1 ? "删除成功" : "删除失败") + "'); window.setTimeout(()=>{window.location='gradePage.do';},1000);</script>");
    }

    public void gradeChange(HttpServletResponse response, HttpServletRequest request) throws ServletException, IOException {
        response.getWriter().println("<style>*{font-size:32px; text-align:center;color:orange;}</style> <script>document.write('" + (GradeServer.SELF.update(new Grade(Integer.parseInt(request.getParameter("gid")), request.getParameter("gname"))) >= 1 ? "修改成功" : "修改失败") + "'); window.setTimeout(()=>{window.location='gradePage.do';},1000);</script>");
    }

    public void examResultPage(HttpServletResponse response, HttpServletRequest request) throws ServletException, IOException {
        int pageNo = StringUtil.intParse(request.getParameter("pageNo"), 1);
        String pageGo = request.getParameter("pageGo");
        if (pageGo != null && pageGo != "") {
            pageNo = Integer.parseInt(pageGo);
        }
        int year = StringUtil.intParse(request.getParameter("year"), 2);
        int stuno = StringUtil.intParse(request.getParameter("stuno"), 0);
        Page<ExamResult> examResultPage = ExamResultServer.SELF.pageByAssociationAndLike(pageNo, Page.PAGE_SIZE, year, stuno);
        List<Student> students = StudentServer.SELF.getAllInfo();
        List<Subject> subjects = SubjectServer.SELF.getAllInfo();
        request.setAttribute("subjects", subjects);
        request.setAttribute("students", students);
        request.setAttribute("useronline", UserListener.userOnline);
        request.setAttribute("year", year == 2 ? "" : year);
        request.setAttribute("stuno", stuno == 0 ? "" : stuno);
        request.setAttribute("examResultPage", examResultPage);
        request.getRequestDispatcher("/WEB-INF/examResult.jsp").forward(request, response);
    }

    public void examResultDelete(HttpServletResponse response, HttpServletRequest request) throws ServletException, IOException {
        response.getWriter().println("<style>*{font-size:32px; text-align:center;color:orange;}</style> <script>document.write('" + (ExamResultServer.SELF.delete(Integer.parseInt(request.getParameter("exaId"))) >= 1 ? "删除成功" : "删除失败") + "'); window.setTimeout(()=>{window.location='examResultPage.do';},1000);</script>");
    }

    public void examResultAdd(HttpServletResponse response, HttpServletRequest request) throws ServletException, IOException {
        response.getWriter().println("<style>*{font-size:32px; text-align:center;color:orange;}</style> <script>document.write('" + (ExamResultServer.SELF.insert(new ExamResult(StringUtil.dateParse(request.getParameter("examDate")), Float.parseFloat(request.getParameter("score")), Integer.parseInt(request.getParameter("stId")), Integer.parseInt(request.getParameter("sbId")))) >= 1 ? "添加成功" : "添加失败") + "'); window.setTimeout(()=>{window.location='examResultPage.do';},1000);</script>");
    }

    public void examResultChange(HttpServletResponse response, HttpServletRequest request) throws ServletException, IOException {
        response.getWriter().println("<style>*{font-size:32px; text-align:center;color:orange;}</style> <script>document.write('" + (ExamResultServer.SELF.update(new ExamResult(Integer.parseInt(request.getParameter("examResultId")), StringUtil.dateParse(request.getParameter("examDate")), Float.parseFloat(request.getParameter("score")), Integer.parseInt(request.getParameter("studentId")), Integer.parseInt(request.getParameter("subjectId")))) >= 1 ? "修改成功" : "修改失败") + "'); window.setTimeout(()=>{window.location='examResultPage.do';},1000);</script>");
    }

    public void checkSubjectName(HttpServletResponse response, HttpServletRequest request) throws ServletException,
            IOException {
        String name = request.getParameter("subjectname");
        boolean repeat = SubjectServer.SELF.checkNameRepeat(name);
        response.getWriter().print(repeat);
    }

    public void showThisExam(HttpServletResponse response, HttpServletRequest request) throws ServletException, IOException {
        int pageNo = StringUtil.intParse(request.getParameter("pageNo"), 1);
        String pageGo = request.getParameter("pageGo");
        if (pageGo != null && pageGo != "") {
            pageNo = Integer.parseInt(pageGo);
        }
        int year = StringUtil.intParse(request.getParameter("year"), 0);
        int stuno = StringUtil.intParse(request.getParameter("stuno"), 0);
        Page<ExamResult> examResultPage = ExamResultServer.SELF.pageByAssociationAndLike(pageNo, Page.PAGE_SIZE, year, stuno);
        List<Student> students = StudentServer.SELF.getAllInfo();
        List<Subject> subjects = SubjectServer.SELF.getAllInfo();
        request.setAttribute("subjects", subjects);
        request.setAttribute("students", students);
        request.setAttribute("useronline", UserListener.userOnline);
        request.setAttribute("year", year == 2 ? "" : year);
        request.setAttribute("stuno", stuno == 0 ? "" : stuno);
        request.setAttribute("examResultPage", examResultPage);
        request.getRequestDispatcher("/WEB-INF/examResult.jsp").forward(request, response);
    }

    public void checkStudentTel(HttpServletResponse response, HttpServletRequest request) throws ServletException, IOException {
        String telephone = request.getParameter("telephone");
        boolean repeat = StudentServer.SELF.checkTelRepeat(telephone);
        response.getWriter().print(repeat);
    }

    public Page<Subject> subjectAjax(HttpServletResponse response, HttpServletRequest request) throws ServletException,
            IOException {
        int pageNo = StringUtil.intParse(request.getParameter("pageNo"), 1);
        String subjectname = StringUtil.nullToBlank(request.getParameter("subjectname"));
        Page<Subject> subjectPage = SubjectServer.SELF.pageByLike(pageNo, Page.PAGE_SIZE, subjectname);
        return subjectPage;
    }

    public int subjectDeleteAjax(HttpServletResponse response, HttpServletRequest request) throws ServletException,
            IOException {
        return SubjectServer.SELF.delete(Integer.parseInt(request.getParameter("subjectId")));
    }

    public Subject getSubjectById(HttpServletResponse response, HttpServletRequest request) throws ServletException, IOException {
        return SubjectServer.SELF.getInfoById(Integer.parseInt(request.getParameter("id")));
    }

    public List<Grade> getAllGrade(HttpServletResponse response, HttpServletRequest request) throws ServletException, IOException {
        return GradeServer.SELF.getAllInfo();
    }

    public void subjectUpdateAjax(HttpServletResponse response, HttpServletRequest request) throws ServletException, IOException {
        response.getWriter().println("<style>*{font-size:32px; text-align:center;color:orange;}</style> <script>document.write('" + (SubjectServer.SELF.update(new Subject(Integer.parseInt(request.getParameter("id")), request.getParameter("subjectname"), Integer.parseInt(request.getParameter("classhour")), Integer.parseInt(request.getParameter("gradeid")))) >= 1 ? "修改成功" : "修改失败") + "'); window.setTimeout(()=>{window.location='subject.html';},1000);</script>");
    }

    public void subjectAddAjax(HttpServletResponse response, HttpServletRequest request) throws ServletException, IOException {
        response.getWriter().println("<style>*{font-size:32px; text-align:center;color:orange;}</style> <script>document.write('" + (SubjectServer.SELF.insert(new Subject(request.getParameter("subjectname"), Integer.parseInt(request.getParameter("classhour")), Integer.parseInt(request.getParameter("gradeid")))) >= 1 ? "添加成功" : "添加失败") + "'); window.setTimeout(()=>{window.location='subject.html';},1000);</script>");
    }
}