package com.share.customer.service.support;

import com.share.common.core.exception.ServiceException;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * 简历文档文本解析与安全清洗抽取器
 */
@Slf4j
@Component
public class ResumeTextExtractor {

    public String extractResumeText(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ServiceException("上传简历文件为空，请选择有效文件");
        }
        String originalFilename = file.getOriginalFilename() != null ? file.getOriginalFilename() : "resume.pdf";
        String lowerName = originalFilename.toLowerCase();

        try {
            byte[] bytes = file.getBytes();
            if (bytes == null || bytes.length == 0) {
                throw new ServiceException("上传文件内容为空");
            }

            // 1. 判断是否为 PDF（根据后缀或魔数 %PDF）
            boolean isPdf = lowerName.endsWith(".pdf") || (bytes.length >= 4 && bytes[0] == 0x25 && bytes[1] == 0x50 && bytes[2] == 0x44 && bytes[3] == 0x46);
            if (isPdf) {
                return extractTextFromPdf(bytes);
            }

            // 2. 判断是否为 DOCX（根据后缀或 ZIP 头 PK\x03\x04）
            boolean isZipOrDocx = lowerName.endsWith(".docx") || (bytes.length >= 4 && bytes[0] == 0x50 && bytes[1] == 0x4B && bytes[2] == 0x03 && bytes[3] == 0x04);
            if (isZipOrDocx) {
                try {
                    String docxText = extractTextFromDocx(bytes);
                    if (StringUtils.hasText(docxText)) {
                        return cleanExtractedText(docxText);
                    }
                } catch (Exception docxEx) {
                    log.warn("DOCX 文本提取失败: {}", docxEx.getMessage());
                }
            }

            // 3. 文本类（TXT, MD 等）
            String text = new String(bytes, StandardCharsets.UTF_8);
            if (isRawBinary(text)) {
                throw new ServiceException("检测到上传文件为二进制格式，无法直接作为纯文本解析，请转换为标准 PDF/Word 文档或复制文本录入");
            }

            String cleaned = cleanExtractedText(text);
            if (StringUtils.hasText(cleaned)) {
                return cleaned;
            }

            return "【已解析简历文件: " + originalFilename + "】\n请在右侧编辑区补充或完善您的核心项目、技能点与履历细节。";
        } catch (ServiceException se) {
            throw se;
        } catch (Exception ex) {
            log.error("简历文件解析异常: {}", ex.getMessage(), ex);
            throw new ServiceException("解析简历文件失败: " + ex.getMessage() + "，建议直接复制简历文本粘贴");
        }
    }

    public String extractTextFromPdf(byte[] bytes) {
        try (PDDocument document = PDDocument.load(new ByteArrayInputStream(bytes))) {
            if (document.isEncrypted()) {
                throw new ServiceException("该 PDF 简历已被加密保护，请先解除密码后再上传");
            }
            PDFTextStripper stripper = new PDFTextStripper();
            stripper.setSortByPosition(true);
            String text = stripper.getText(document);
            String cleaned = cleanExtractedText(text);
            if (!StringUtils.hasText(cleaned)) {
                throw new ServiceException("该 PDF 简历未包含可提取的文字层（可能为图片扫描件），请直接复制简历文字粘贴录入");
            }
            return cleaned;
        } catch (ServiceException se) {
            throw se;
        } catch (Exception ex) {
            log.error("PDFBox 提取 PDF 失败: {}", ex.getMessage(), ex);
            throw new ServiceException("PDF 文件解析失败，请检查文件是否损坏或直接粘贴文字");
        }
    }

    public String extractTextFromDocx(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        try (ZipInputStream zis = new ZipInputStream(new ByteArrayInputStream(bytes))) {
            ZipEntry entry;
            int entryCount = 0;
            long totalUncompressedSize = 0;
            // 限制最大解压体积：输入体积的100倍，且硬上限不超过 20MB
            long maxAllowedSize = Math.min(Math.max((long) bytes.length * 100L, 10 * 1024 * 1024L), 20 * 1024 * 1024L);
            while ((entry = zis.getNextEntry()) != null) {
                entryCount++;
                if (entryCount > 1000) {
                    throw new ServiceException("ZIP压缩包条目过多（超过1000条），可能存在解压炸弹攻击风险");
                }
                String entryName = entry.getName();
                if (entryName != null && entryName.contains("..")) {
                    continue; // 抵御 Zip Slip 路径穿越
                }
                if ("word/document.xml".equalsIgnoreCase(entryName)) {
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    byte[] buffer = new byte[4096];
                    int len;
                    while ((len = zis.read(buffer)) != -1) {
                        totalUncompressedSize += len;
                        if (totalUncompressedSize > maxAllowedSize) {
                            throw new ServiceException("文档解压后体积异常膨胀，拒绝处理以防内存耗尽（Zip Bomb）");
                        }
                        baos.write(buffer, 0, len);
                    }
                    String xml = baos.toString(StandardCharsets.UTF_8);
                    // 将段落标签 </w:p> 和换行符 <w:br/> 替换为真正的换行
                    xml = xml.replaceAll("</w:p>", "\n");
                    xml = xml.replaceAll("<w:br[^>]*/>", "\n");
                    xml = xml.replaceAll("<w:tab[^>]*/>", "\t");
                    // 剥离其余所有 XML 标签
                    xml = xml.replaceAll("<[^>]+>", "");
                    // 反转义常见实体
                    xml = xml.replace("&amp;", "&")
                             .replace("&lt;", "<")
                             .replace("&gt;", ">")
                             .replace("&quot;", "\"")
                             .replace("&apos;", "'");
                    sb.append(xml);
                    break;
                }
            }
        } catch (ServiceException se) {
            throw se;
        } catch (Exception ex) {
            log.warn("解析 Word document.xml 异常: {}", ex.getMessage());
        }
        return sb.toString();
    }

    public String cleanExtractedText(String raw) {
        if (!StringUtils.hasText(raw)) {
            return "";
        }
        // 过滤 NULL 字节与不合法的控制字符
        String cleaned = raw.replaceAll("[\\x00-\\x08\\x0B\\x0C\\x0E-\\x1F]", "");
        cleaned = cleaned.replace("\r\n", "\n").replace("\r", "\n");
        // 压缩过多的连续空行
        cleaned = cleaned.replaceAll("\n{3,}", "\n\n");
        cleaned = cleaned.trim();
        // 限制最大字数，避免超出数据库列容量
        if (cleaned.length() > 15000) {
            cleaned = cleaned.substring(0, 15000) + "\n\n【系统提示：已自动保留简历前 15,000 字核心履历】";
        }
        return cleaned;
    }

    public boolean isRawBinary(String text) {
        if (!StringUtils.hasText(text)) {
            return false;
        }
        if (text.startsWith("%PDF-") || text.startsWith("PK\u0003\u0004")) {
            return true;
        }
        int unprintable = 0;
        int checkLen = Math.min(text.length(), 300);
        for (int i = 0; i < checkLen; i++) {
            char c = text.charAt(i);
            if (c != '\n' && c != '\r' && c != '\t' && c < 0x20) {
                unprintable++;
            }
        }
        return unprintable > 5;
    }
}
