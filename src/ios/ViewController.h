//
//  ViewController.h
//  OpenCVTest
//
//  Created by DNVA on 23/03/16.
//  Copyright © 2016 DNVA. All rights reserved.
//

#import <UIKit/UIKit.h>
#import <opencv2/highgui/cap_ios.h>

@interface ViewController : UIViewController<CvVideoCameraDelegate>
{
    IBOutlet UIImageView *img;
    CvVideoCamera *camera;
}

@property (nonatomic, retain) CvVideoCamera *camera;

@end

