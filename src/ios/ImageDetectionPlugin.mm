#import "ImageDetectionPlugin.h"
#import "ImageUtils.h"
#import <opencv2/highgui/ios.h>
#import <opencv2/features2d/features2d.hpp>
#import <opencv2/nonfree/nonfree.hpp>

using namespace cv;

@interface ImageDetectionPlugin()
{
    Mat patt, desc1;
    vector<KeyPoint> kp1;
    bool debug, thread_over;
    NSMutableArray *detection;
}

@end

@implementation ImageDetectionPlugin

@synthesize camera, img;

- (void)greet:(CDVInvokedUrlCommand*)command
{
    [self.commandDelegate runInBackground:^{
        CDVPluginResult* plugin_result = nil;
        NSString* name = [command.arguments objectAtIndex:0];
        NSString* msg = [NSString stringWithFormat: @"Hello, %@", name];

        if (name != nil && [name length] > 0) {
            plugin_result = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsString:msg];
        } else {
            plugin_result = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR];
        }

        [self.commandDelegate sendPluginResult:plugin_result callbackId:command.callbackId];
    }];
}

-(void)isDetecting:(CDVInvokedUrlCommand*)command
{
    [self.commandDelegate runInBackground:^{
        CDVPluginResult* plugin_result = nil;
        NSString* msg;

        if ([self getState]) {
            msg = @"pattern detected";
            plugin_result = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsString:msg];
        } else {
            msg = @"pattern not detected";
            plugin_result = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR messageAsString:msg];
        }

        [self.commandDelegate sendPluginResult:plugin_result callbackId:command.callbackId];
    }];
}

- (void)setPattern:(CDVInvokedUrlCommand*)command;
{
    [self.commandDelegate runInBackground:^{
        CDVPluginResult* plugin_result = nil;
        NSString* pattern = [command.arguments objectAtIndex:0];
        NSString* msg;

        if (pattern != nil && [pattern length] > 0) {
            [self setBase64Pattern: pattern];
            msg = @"pattern selected";
            plugin_result = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsString:msg];
        } else {
            msg = @"a pattern must be set";
            plugin_result = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR messageAsString:msg];
        }

        [self.commandDelegate sendPluginResult:plugin_result callbackId:command.callbackId];
    }];
}

-(void)setBase64Pattern:(NSString *)image_base64
{
    if ([image_base64 rangeOfString:@"data:"].location == NSNotFound) {
        // do nothing
    } else {
        NSArray *lines = [image_base64 componentsSeparatedByString: @","];
        image_base64 = lines[1];
    }

    UIImage *image = [ImageUtils decodeBase64ToImage: image_base64];
    patt = [ImageUtils cvMatFromUIImage: image];
    cvtColor(patt, patt, CV_BGRA2GRAY);
    ORB orb;
    orb.detect(patt, kp1);
    orb.compute(patt, kp1, desc1);
}

- (void)pluginInitialize {
    // set orientation portraint
    NSNumber *value = [NSNumber numberWithInt:UIInterfaceOrientationPortrait];
    [[UIDevice currentDevice] setValue:value forKey:@"orientation"];

    // set webview and it's subviews to transparent
    for (UIView *subview in [self.webView subviews]) {
        [subview setOpaque:NO];
        [subview setBackgroundColor:[UIColor clearColor]];
    }
    [self.webView setBackgroundColor:[UIColor clearColor]];
    [self.webView setOpaque: NO];
    // setup view to render the camera capture
    CGRect screenRect = [[UIScreen mainScreen] bounds];
    img = [[UIImageView alloc] initWithFrame: screenRect];
    img.contentMode = UIViewContentModeScaleAspectFill;
    //img.transform = CGAffineTransformMakeRotation((-90 * M_PI) / 180);
    [self.webView.superview addSubview: img];
    // set views order
    [self.webView.superview bringSubviewToFront: self.webView];

    //Camera
    self.camera = [[CvVideoCamera alloc] initWithParentView: img];
    self.camera.defaultAVCaptureDevicePosition = AVCaptureDevicePositionBack;
    self.camera.defaultAVCaptureSessionPreset = AVCaptureSessionPresetMedium;
    self.camera.defaultAVCaptureVideoOrientation = AVCaptureVideoOrientationPortrait;
    self.camera.defaultFPS = 30;
    self.camera.grayscaleMode = NO;
    self.camera.rotateVideo = YES;

    self.camera.delegate = self;

    debug = false;
    thread_over = true;

    detection = [[NSMutableArray alloc] init];

    [self.camera start];
    NSLog(@"----------- CAMERA STARTED ----------");
}

#pragma mark - Protocol CvVideoCameraDelegate
#ifdef __cplusplus
- (void)processImage:(Mat &)image;
{
    // process each image in new thread
    if(!image.empty() && thread_over){
        thread_over = false;
        Mat image_copy = image.clone();
        dispatch_async(dispatch_get_global_queue(DISPATCH_QUEUE_PRIORITY_DEFAULT, 0), ^{
            [self backgroundImageProcessing: image_copy];
            dispatch_sync(dispatch_get_main_queue(), ^{
                thread_over = true;
            });
        });
    }
}
#endif

#ifdef __cplusplus
- (void)backgroundImageProcessing:(const Mat &)image
{
    if(!image.empty() && !patt.empty())
    {
        Mat gray = image;
        //Mat image_copy = image;
        Mat desc2;
        vector<KeyPoint> kp2;

        cvtColor(image, gray, CV_BGRA2GRAY);

        ORB orb;
        orb.detect(gray, kp2);
        orb.compute(gray, kp2, desc2);

        BFMatcher bf = BFMatcher::BFMatcher(NORM_HAMMING2, true);
        vector<DMatch> matches;
        vector<DMatch> good_matches;

        if(!desc1.empty() && !desc2.empty())
        {
            bf.match(desc1, desc2, matches);

            int size = 0;
            double min_dist = 1000;
            if(desc1.rows < matches.size())
                size = desc1.rows;
            else
                size = (int)matches.size();

            for(int i = 0; i < size; i++)
            {
                double dist = matches[i].distance;
                if(dist < min_dist)
                {
                    min_dist = dist;
                }
            }

            vector<DMatch> good_matches_reduced;

            for(int i = 0; i < size; i++)
            {
                if(matches[i].distance <=  2 * min_dist && good_matches.size() < 500)
                {
                    good_matches.push_back(matches[i]);
                    if(i < 10 && debug)
                    {
                        good_matches_reduced.push_back(matches[i]);
                    }
                }
            }

            if(good_matches.size() >= 8)
            {
                if(debug)
                {
                    Mat imageMatches;
                    drawMatches(patt, kp1, gray, kp2, good_matches_reduced, imageMatches, Scalar::all(-1), Scalar::all(-1), vector<char>(), DrawMatchesFlags::NOT_DRAW_SINGLE_POINTS);
                    //image_copy = imageMatches;
                }

                Mat img_matches = image;
                //-- Localize the object
                vector<Point2f> obj;
                vector<Point2f> scene;

                for( int i = 0; i < good_matches.size(); i++ )
                {
                    //-- Get the keypoints from the good matches
                    obj.push_back( kp1[ good_matches[i].queryIdx ].pt );
                    scene.push_back( kp2[ good_matches[i].trainIdx ].pt );
                }

                Mat H = findHomography( obj, scene, CV_RANSAC);

                bool result = true;

                const double det = H.at<double>(0, 0) * H.at<double>(1, 1) - H.at<double>(1, 0) * H.at<double>(0, 1);
                if (det < 0)
                    result = false;

                const double N1 = sqrt(H.at<double>(0, 0) * H.at<double>(0, 0) + H.at<double>(1, 0) * H.at<double>(1, 0));
                if (N1 > 4 || N1 < 0.1)
                    result =  false;

                const double N2 = sqrt(H.at<double>(0, 1) * H.at<double>(0, 1) + H.at<double>(1, 1) * H.at<double>(1, 1));
                if (N2 > 4 || N2 < 0.1)
                    result = false;

                const double N3 = sqrt(H.at<double>(2, 0) * H.at<double>(2, 0) + H.at<double>(2, 1) * H.at<double>(2, 1));
                if (N3 > 0.002)
                    result = false;

                //NSLog(@"det %f, N1 %f, N2 %f, N3 %f, result %i", det, N1, N2, N3, result);

                if(result)
                {
                    //NSLog(@"detecting");
                    [self updateState: true];

                    if(debug)
                    {
                        //-- Get the corners from the image_1 ( the object to be "detected" )
                        vector<Point2f> obj_corners(4);
                        obj_corners[0] = cvPoint(0,0); obj_corners[1] = cvPoint( patt.cols, 0 );
                        obj_corners[2] = cvPoint( patt.cols, patt.rows ); obj_corners[3] = cvPoint( 0, patt.rows );
                        vector<Point2f> scene_corners(4);

                        perspectiveTransform( obj_corners, scene_corners, H);

                        //-- Draw lines between the corners (the mapped object in the scene - image_2 )
                        line( img_matches, scene_corners[0] + Point2f( patt.cols, 0), scene_corners[1] + Point2f( patt.cols, 0), Scalar(0, 255, 0), 4 );
                        line( img_matches, scene_corners[1] + Point2f( patt.cols, 0), scene_corners[2] + Point2f( patt.cols, 0), Scalar( 0, 255, 0), 4 );
                        line( img_matches, scene_corners[2] + Point2f( patt.cols, 0), scene_corners[3] + Point2f( patt.cols, 0), Scalar( 0, 255, 0), 4 );
                        line( img_matches, scene_corners[3] + Point2f( patt.cols, 0), scene_corners[0] + Point2f( patt.cols, 0), Scalar( 0, 255, 0), 4 );

                        //image_copy = img_matches;
                    }
                } else {
                    [self updateState: false];
                }
                H.release();
                img_matches.release();
            }
            matches.clear();
            good_matches.clear();
            good_matches_reduced.clear();
        }
        gray.release();
        desc2.release();
        kp2.clear();
        //image = image_copy;
    }
}
#endif

-(void)updateState:(BOOL) state
{
    if(detection.count > 15)
    {
        [detection removeObjectAtIndex:0];
    }

    if(state)
    {
        [detection addObject:[NSNumber numberWithBool:YES]];
    } else {
        [detection addObject:[NSNumber numberWithBool:NO]];
    }
}

-(BOOL)getState
{
    NSNumber *total = 0;
    for (NSNumber *states in detection){
        total = [NSNumber numberWithInt:([total intValue] + [states intValue])];
    }
    if ([total intValue] >= 2) {
        return true;
    } else {
        return false;
    }
}
@end
