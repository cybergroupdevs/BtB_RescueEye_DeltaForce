import os
import time
import glob
# web framework 
from tornado.ioloop import IOLoop
from tornado.web import Application, RequestHandler, StaticFileHandler
import global_config as g
import train as t



class basicRequestHandler(RequestHandler):
    def get(self):
        self.render("index.htm")


class uploadRequestHandler(RequestHandler):
    def get(self):
        self.render("index.htm")

    def post(self):
        selType = self.get_argument('selType')
        dateStamp = time.strftime('%Y-%m-%d', time.gmtime())

        # Create target directory & all intermediate directories if don't exists
        if not os.path.exists(g.VIDEO_PATH):
            os.makedirs(g.VIDEO_PATH)

        files = self.request.files["imgFile"]
        for f in files:
            index = f.filename.find('.')
            fn = f"{g.VIDEO_PATH}/{dateStamp}_{f.filename[:index]}_{selType}{ f.filename[index:]}"
            fh = open(f"{fn}", "wb")
            fh.write(f.body)
            fh.close()
            t.learn(fn,selType)

class downloadModelRequestHandler(RequestHandler):
    def get(self):
        self.set_header('Content-Type', 'application/octet-stream')
        self.set_header('Content-Disposition', 'attachment; filename=' + g.TENSOR_LITE_MODEL_FILENAME)
        list_of_folders = glob.glob(os.path.join(g.TENSOR_MODEL_PATH,  f'*{g.TENSOR_MODEL_SUFFIX}'))
        latest_folder = None
        if len(list_of_folders) > 0:
            # Get Latest Model
            latest_folder = max(list_of_folders, key=os.path.getctime)
            
        if latest_folder != None:
            with open(f'{latest_folder}/{g.TENSOR_LITE_MODEL_FILENAME}', 'rb') as f:
                self.write(f.read())
        self.finish()



if __name__ == "__main__":
    app = Application([
        (r"/", uploadRequestHandler),
        (r"/download", downloadModelRequestHandler),
        (r"/img/(.*)", StaticFileHandler, {"path": "assets/img"}),
        (r"/css/(.*)", StaticFileHandler, {"path": "assets/css"}),
        (r"/scripts/(.*)", StaticFileHandler,
         {"path": "assets/scripts"}),
        (r"/videos/(.*)", StaticFileHandler, {"path": "assets/videos"})
    ])

    app.listen(8881)
    print("Listening on port:8881")
    IOLoop.current().start()
