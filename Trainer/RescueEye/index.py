import os
import time

from tornado.ioloop import IOLoop
from tornado.web import Application, RequestHandler, StaticFileHandler


class basicRequestHandler(RequestHandler):
    def get(self):
        self.render("index.htm")


class uploadRequestHandler(RequestHandler):
    def get(self):
        self.render("index.htm")

    def post(self):
        selType = self.get_argument('selType')
        dirName = f'assets/img/{selType}'
        # Create target directory & all intermediate directories if don't exists
        if not os.path.exists(dirName):
            os.makedirs(dirName)

        files = self.request.files["imgFile"]
        for f in files:
            index = f.filename.find('.')
            ts = time.gmtime()
            # append timestamp and open file
            fn = f"{f.filename[:index]}_{time.strftime('%Y-%m-%d_%H-%M-%S', ts)}{ f.filename[index:]}"
            fh = open(f"{dirName}/{fn}", "wb")
            fh.write(f.body)
            fh.close()

        self.write(f"http://localhost:8881/img/{selType}/{fn}")
        # send this path to training model asynchronously

        # and save the response in file system under /models


# class trainRequestHandler(RequestHandler):

#     def post(self):
#         print(self)
#         print(self.request)
#         print(self.request.arguments)
#         print(self.request.arguments)
#         print(self.get_argument("selType"))


if __name__ == "__main__":
    app = Application([
        (r"/", uploadRequestHandler),
        # (r"/train", trainRequestHandler),
        (r"/img/(.*)", StaticFileHandler, {"path": "assets/img"}),
        (r"/css/(.*)", StaticFileHandler, {"path": "assets/css"}),
        (r"/scripts/(.*)", StaticFileHandler,
         {"path": "assets/scripts"})
    ])

    app.listen(8881)
    print("Listening on port:8881")
    IOLoop.current().start()
