"""更新原始 Word 后，使用 --soffice 指定 LibreOffice 可执行文件重新生成预览。"""
import argparse
from pathlib import Path
import shutil
import subprocess
import tempfile


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--soffice", required=True)
    args = parser.parse_args()
    system = Path(__file__).resolve().parents[1]
    originals = system / "src/main/template-originals"
    previews = system / "src/main/resources/templates/document/preview"
    # 每次使用独立的 Office 配置和输出目录，不连接已有的编辑会话。
    with tempfile.TemporaryDirectory(prefix="template-preview-") as temp:
        work = Path(temp)
        for source in sorted(originals.rglob("*.docx")):
            subprocess.run([
                args.soffice, "-env:UserInstallation=" + (work / "profile").as_uri(),
                "--headless", "--convert-to", "pdf:writer_pdf_Export",
                "--outdir", str(work), str(source),
            ], check=True, timeout=120)
            pdf = work / (source.stem + ".pdf")
            if not pdf.is_file():
                raise RuntimeError("生成模板预览失败：" + str(source))
            target = previews / source.relative_to(originals).with_suffix(".pdf")
            target.parent.mkdir(parents=True, exist_ok=True)
            shutil.copy2(pdf, target)
            pdf.unlink()


if __name__ == "__main__":
    main()
